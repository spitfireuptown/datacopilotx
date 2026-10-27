package com.datacopilotx.aigateway.service.chat;

import cn.hutool.json.JSONUtil;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.domain.dto.OllamaResultDTO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class OllamaAIChatService implements AIChatService {

    private final Map<String, WebClient> webClientCache = new ConcurrentHashMap<>();

    private WebClient getWebClient() {
        return webClientCache.computeIfAbsent("ollama", k -> WebClient.builder().build());
    }

    @Override
    public String chatCompletions(ChatRequest chatRequest) {
        String question = chatRequest.getQuestion() != null ? chatRequest.getQuestion().trim() : "";
        Map<String, Object> bodyJson = Map.of(
                "model", chatRequest.getModel(),
                "prompt", question,
                "stream", false
        );

        long startTime = System.currentTimeMillis();
        OllamaResultDTO result = getWebClient()
                .post()
                .uri(chatRequest.getBaseUrl())
                .body(BodyInserters.fromValue(bodyJson))
                .retrieve()
                .bodyToMono(OllamaResultDTO.class)
                .block();
        // 记录 Token 用量与耗时，供调用方统计
        if (result != null && (result.getPromptEvalCount() != null || result.getEvalCount() != null)) {
            int tokenUsage = (result.getPromptEvalCount() != null ? result.getPromptEvalCount() : 0)
                    + (result.getEvalCount() != null ? result.getEvalCount() : 0);
            chatRequest.setTokenUsage(tokenUsage);
        }
        chatRequest.setTimeCost(System.currentTimeMillis() - startTime);
        return result.getMessage().getContent().toString();
    }

    @Override
    public Flux<String> streamChatCompletions(ChatRequest chatRequest) {
        long startTime = System.currentTimeMillis();

        String systemPrompt = chatRequest.getSystemPrompt() != null ? chatRequest.getSystemPrompt().trim() : "";
        String userPrompt = chatRequest.getUserPrompt() != null ? chatRequest.getUserPrompt().trim() : "";
        Map<String, Object> bodyJson = Map.of(
                "model", chatRequest.getModel(),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", systemPrompt + "\n" + userPrompt
                )),
                "stream", true,
                "think", false
        );

        chatRequest.setTokenUsage(0);

        return getWebClient()
                .post()
                .uri(chatRequest.getBaseUrl())
                .body(BodyInserters.fromValue(bodyJson))
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(data -> {
                    OllamaResultDTO ollamaResultDTO = JSONUtil.toBean(data, OllamaResultDTO.class);
                    // 最后一个分片会携带 Token 统计，覆盖默认值
                    if (ollamaResultDTO.getPromptEvalCount() != null || ollamaResultDTO.getEvalCount() != null) {
                        int tokenUsage = (ollamaResultDTO.getPromptEvalCount() != null ? ollamaResultDTO.getPromptEvalCount() : 0)
                                + (ollamaResultDTO.getEvalCount() != null ? ollamaResultDTO.getEvalCount() : 0);
                        chatRequest.setTokenUsage(tokenUsage);
                    }
                    return Flux.just(ollamaResultDTO.getMessage().getContent());
                })
                .doOnError(error -> log.error(error.getMessage(), error))
                .doOnComplete(() -> chatRequest.setTimeCost(System.currentTimeMillis() - startTime));
    }

    @Override
    public List<Float> embedding(ChatRequest chatRequest) {
        String question = chatRequest.getQuestion() != null ? chatRequest.getQuestion().trim() : "";
        Map<String, Object> bodyJson = Map.of(
                "model", chatRequest.getModel(),
                "input", question,
                "prompt", question
        );
        return getWebClient()
                .post()
                .uri(chatRequest.getBaseUrl())
                .body(BodyInserters.fromValue(bodyJson))
                .retrieve()
                .bodyToMono(OllamaResultDTO.EmbeddingResultDTO.class)
                .block()
                .getEmbedding();
    }
}