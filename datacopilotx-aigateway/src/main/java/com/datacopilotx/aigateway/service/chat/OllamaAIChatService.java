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
        Map<String, Object> bodyJson = Map.of(
                "model", chatRequest.getModel(),
                "prompt", chatRequest.getQuestion(),
                "stream", false
        );

        return getWebClient()
                .post()
                .uri(chatRequest.getBaseUrl())
                .body(BodyInserters.fromValue(bodyJson))
                .retrieve()
                .bodyToMono(OllamaResultDTO.class)
                .block()
                .getMessage()
                .getContent()
                .toString();
    }

    @Override
    public Flux<String> streamChatCompletions(ChatRequest chatRequest) {
        long startTime = System.currentTimeMillis();

        Map<String, Object> bodyJson = Map.of(
                "model", chatRequest.getModel(),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", chatRequest.getSystemPrompt() + "\n" + chatRequest.getUserPrompt()
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
                    return Flux.just(ollamaResultDTO.getMessage().getContent());
                })
                .doOnError(error -> log.error(error.getMessage(), error))
                .doOnComplete(() -> chatRequest.setTimeCost(System.currentTimeMillis() - startTime));
    }

    @Override
    public List<Float> embedding(ChatRequest chatRequest) {
        Map<String, Object> bodyJson = Map.of(
                "model", chatRequest.getModel(),
                "input", chatRequest.getQuestion(),
                "prompt", chatRequest.getQuestion()
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