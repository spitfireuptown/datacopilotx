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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static dev.langchain4j.data.message.SystemMessage.systemMessage;
import static dev.langchain4j.data.message.UserMessage.userMessage;
import static java.util.Arrays.asList;

@Slf4j
@Service
public class OpenAIChatService implements AIChatService {

    @Override
    public String chatCompletions(ChatRequest chatRequest) {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(chatRequest.getApiKey())
                .modelName(chatRequest.getModel())
                .baseUrl(chatRequest.getBaseUrl())
                .temperature(0.0)
                .topP(1.0)
                .build();
        return chatModel.chat(chatRequest.getQuestion());
    }

    @Override
    public Flux<String> streamChatCompletions(ChatRequest chatRequest) {
        OpenAiStreamingChatModel streamingChatModel = OpenAiStreamingChatModel.builder()
                .apiKey(chatRequest.getApiKey())
                .modelName(chatRequest.getModel())
                .baseUrl(chatRequest.getBaseUrl())
                .temperature(0.0)
                .topP(1.0)
                .build();

        List<ChatMessage> messages = asList(
                systemMessage(chatRequest.getSystemPrompt()),
                userMessage(chatRequest.getUserPrompt())
        );
        long startTime = System.currentTimeMillis();
        return Flux.create(sink -> {
            streamingChatModel.chat(messages, new StreamingChatResponseHandler() {

                @Override
                public void onPartialResponse(String partialResponse) {
                    sink.next(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    chatRequest.setTokenUsage(completeResponse.tokenUsage().totalTokenCount());
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
        OpenAiEmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(chatRequest.getApiKey())
                .modelName(chatRequest.getModel())
                .baseUrl(chatRequest.getBaseUrl())
                .dimensions(chatRequest.getDimensions())
                .build();


        TextSegment segment = TextSegment.from(chatRequest.getQuestion());
        Embedding embedding = embeddingModel.embed(segment).content();
        return Floats.asList(embedding.vector());
    }
}
