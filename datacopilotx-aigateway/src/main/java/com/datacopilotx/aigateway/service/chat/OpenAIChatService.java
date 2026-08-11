package com.datacopilotx.aigateway.service.chat;

import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.common.exception.DataCopilotXException;
import com.datacopilotx.common.result.ResponseCode;
import com.google.common.primitives.Floats;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static dev.langchain4j.data.message.SystemMessage.systemMessage;
import static dev.langchain4j.data.message.UserMessage.userMessage;
import static java.util.Arrays.asList;

@Slf4j
@Service
public class OpenAIChatService implements AIChatService {

    private static final Map<String, OpenAiChatModel> chatModelCache = new ConcurrentHashMap<>();
    private static final Map<String, OpenAiStreamingChatModel> streamingChatModelCache = new ConcurrentHashMap<>();
    private static final Map<String, OpenAiEmbeddingModel> embeddingModelCache = new ConcurrentHashMap<>();

    /**
     * OpenAI HTTP 请求超时时间（秒），默认 120s，覆盖 langchain4j 默认的 60s 读超时。
     * NL2SQL 等长文本生成任务可能超过 60s，需通过 ai.gateway.openai.timeout-seconds 调整。
     */
    @Value("${ai.gateway.openai.timeout-seconds:120}")
    private long timeoutSeconds;

    private String buildModelKey(String apiKey, String model, String baseUrl) {
        return String.format("%s_%s_%s", apiKey, model, baseUrl);
    }

    private OpenAiChatModel getChatModel(ChatRequest chatRequest) {
        String key = buildModelKey(chatRequest.getApiKey(), chatRequest.getModel(), chatRequest.getBaseUrl());
        return chatModelCache.computeIfAbsent(key, k -> OpenAiChatModel.builder()
                .apiKey(chatRequest.getApiKey())
                .modelName(chatRequest.getModel())
                .baseUrl(chatRequest.getBaseUrl())
                .temperature(0.0)
                .topP(1.0)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build());
    }

    private OpenAiStreamingChatModel getStreamingChatModel(ChatRequest chatRequest) {
        String key = buildModelKey(chatRequest.getApiKey(), chatRequest.getModel(), chatRequest.getBaseUrl());
        return streamingChatModelCache.computeIfAbsent(key, k -> OpenAiStreamingChatModel.builder()
                .apiKey(chatRequest.getApiKey())
                .modelName(chatRequest.getModel())
                .baseUrl(chatRequest.getBaseUrl())
                .temperature(0.0)
                .topP(1.0)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build());
    }

    private OpenAiEmbeddingModel getEmbeddingModel(ChatRequest chatRequest) {
        String key = buildModelKey(chatRequest.getApiKey(), chatRequest.getModel(), chatRequest.getBaseUrl());
        return embeddingModelCache.computeIfAbsent(key, k -> OpenAiEmbeddingModel.builder()
                .apiKey(chatRequest.getApiKey())
                .modelName(chatRequest.getModel())
                .baseUrl(chatRequest.getBaseUrl())
                .dimensions(chatRequest.getDimensions())
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build());
    }

    @Override
    public String chatCompletions(ChatRequest chatRequest) {
        OpenAiChatModel chatModel = getChatModel(chatRequest);
        String userMessageContent = chatRequest.getUserPrompt();
        if (userMessageContent == null || userMessageContent.trim().isEmpty()) {
            userMessageContent = chatRequest.getQuestion();
        }
        userMessageContent = userMessageContent != null ? userMessageContent.trim() : "";

        String systemMessageContent = chatRequest.getSystemPrompt();

        List<ChatMessage> messages;
        if (systemMessageContent != null && !systemMessageContent.trim().isEmpty()) {
            messages = asList(
                    systemMessage(systemMessageContent),
                    userMessage(userMessageContent)
            );
        } else {
            messages = List.of(userMessage(userMessageContent));
        }
        return chatModel.chat(messages).aiMessage().text();
    }

    @Override
    public Flux<String> streamChatCompletions(ChatRequest chatRequest) {
        OpenAiStreamingChatModel streamingChatModel = getStreamingChatModel(chatRequest);

        String userMessageContent = chatRequest.getUserPrompt();
        if (userMessageContent == null || userMessageContent.trim().isEmpty()) {
            userMessageContent = chatRequest.getQuestion();
        }
        userMessageContent = userMessageContent != null ? userMessageContent.trim() : "";

        String systemMessageContent = chatRequest.getSystemPrompt();

        List<ChatMessage> messages;
        if (systemMessageContent != null && !systemMessageContent.trim().isEmpty()) {
            messages = asList(
                    systemMessage(systemMessageContent),
                    userMessage(userMessageContent)
            );
        } else {
            messages = List.of(userMessage(userMessageContent));
        }
        long startTime = System.currentTimeMillis();
        return Flux.create(sink -> {
            streamingChatModel.chat(messages, new StreamingChatResponseHandler() {

                @Override
                public void onPartialResponse(String partialResponse) {
                    sink.next(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    if (completeResponse != null && completeResponse.tokenUsage() != null) {
                        chatRequest.setTokenUsage(completeResponse.tokenUsage().totalTokenCount());
                    }
                    chatRequest.setTimeCost(System.currentTimeMillis() - startTime);
                    sink.complete();
                }

                @Override
                public void onError(Throwable error) {
                    log.error("chatCompletionserror#", error);
                    sink.error(error);
                }
            });
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    @Override
    public List<Float> embedding(ChatRequest chatRequest) {
        OpenAiEmbeddingModel embeddingModel = getEmbeddingModel(chatRequest);

        String question = chatRequest.getQuestion() != null ? chatRequest.getQuestion().trim() : "";
        TextSegment segment = TextSegment.from(question);
        Embedding embedding = embeddingModel.embed(segment).content();
        return Floats.asList(embedding.vector());
    }
}