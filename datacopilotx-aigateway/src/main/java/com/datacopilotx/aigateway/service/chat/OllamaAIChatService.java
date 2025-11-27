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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OllamaAIChatService implements AIChatService {

    public WebClient.Builder webClient;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder();
    }

    @Override
    public String chatCompletions(ChatRequest chatRequest) {
        Map<String, Object> bodyJson = new HashMap<>();
        bodyJson.put("model", chatRequest.getModel());
        bodyJson.put("prompt", chatRequest.getQuestion());
        bodyJson.put("stream", false);


        return webClient.build()
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
        Map<String, Object> bodyJson = new HashMap<>();
        long startTime = System.currentTimeMillis();

        bodyJson.put("model", chatRequest.getModel());
        bodyJson.put("messages",
                List.of(
                    Map.of(
                    "role", "user",
                    "content", chatRequest.getSystemPrompt() + "\n" + chatRequest.getUserPrompt()
                    )
                )
        );
        bodyJson.put("stream", true);
        bodyJson.put("think", false);

        // ollama通常使用本地模型，暂不统计token消耗
        chatRequest.setTokenUsage(0);

        return webClient.build()
                .post()
                .uri(chatRequest.getBaseUrl())
                .body(BodyInserters.fromValue(bodyJson))
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap((data) -> {
                    OllamaResultDTO ollamaResultDTO = JSONUtil.toBean(data, OllamaResultDTO.class);
                    return Flux.just(ollamaResultDTO.getMessage().getContent());
                })
                .doOnError((error) -> log.error(error.getMessage(), error))
                .doOnComplete(() -> chatRequest.setTimeCost(System.currentTimeMillis() - startTime));
    }


    @Override
    public List<Float> embedding(ChatRequest chatRequest) {
        Map<String, Object> bodyJson = new HashMap<>();
        bodyJson.put("model", chatRequest.getModel());
        bodyJson.put("input", chatRequest.getQuestion());
        bodyJson.put("prompt", chatRequest.getQuestion());
        return webClient.build()
                .post()
                .uri(chatRequest.getBaseUrl())
                .body(BodyInserters.fromValue(bodyJson))
                .retrieve()
                .bodyToMono(OllamaResultDTO.EmbeddingResultDTO.class)
                .block()
                .getEmbedding();
    }
}
