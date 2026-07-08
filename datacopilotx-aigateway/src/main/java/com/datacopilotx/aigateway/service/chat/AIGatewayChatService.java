package com.datacopilotx.aigateway.service.chat;


import com.datacopilotx.aigateway.config.RetryConfig;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.util.RetryHelper;
import com.datacopilotx.common.constant.GlobalConstant;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class AIGatewayChatService implements AIChatService, ApplicationContextAware {

    public Map<String, AIChatService> messageHubServiceMap = new ConcurrentHashMap<>();
    private ApplicationContext applicationContext;
    private final RetryConfig retryConfig;

    public AIGatewayChatService(RetryConfig retryConfig) {
        this.retryConfig = retryConfig;
    }

    @PostConstruct
    public void init() {
        this.messageHubServiceMap.put(GlobalConstant.MODEL_OPEN_AI, applicationContext.getBean(OpenAIChatService.class));
        this.messageHubServiceMap.put(GlobalConstant.MODEL_OLLAMA, applicationContext.getBean(OllamaAIChatService.class));
    }

    private AIChatService aiModelFactory(String type) {
        return this.messageHubServiceMap.get(type);
    }

    @Override
    public String chatCompletions(ChatRequest chatRequest) {
        return this.aiModelFactory(chatRequest.getType()).chatCompletions(chatRequest);
    }

    @Override
    public Flux<String> streamChatCompletions(ChatRequest chatRequest) {
        return Flux.defer(() -> {
            AtomicBoolean dataEmitted = new AtomicBoolean(false);

            Retry retrySpec = Retry.backoff(retryConfig.getMaxAttempts(), Duration.ofMillis(retryConfig.getMinBackoff()))
                    .maxBackoff(Duration.ofMillis(retryConfig.getMaxBackoff()))
                    .jitter(0.5)
                    .doBeforeRetry(retrySignal -> log.warn("Retrying stream chat (attempt {}), error: {}",
                            retrySignal.totalRetries() + 1, retrySignal.failure().getMessage()))
                    .filter(throwable -> {
                        if (dataEmitted.get()) {
                            log.warn("Data already emitted, will not retry");
                            return false;
                        }
                        return RetryHelper.RETRYABLE_ERROR_PREDICATE.test(throwable);
                    })
                    .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                        log.error("Stream chat completions exhausted after {} attempts", retrySignal.totalRetries());
                        return retrySignal.failure();
                    });

            return this.aiModelFactory(chatRequest.getType()).streamChatCompletions(chatRequest)
                    .doOnNext(s -> dataEmitted.set(true))
                    .retryWhen(retrySpec);
        });
    }


    @Override
    public List<Float> embedding(ChatRequest chatRequest) {
        return this.aiModelFactory(chatRequest.getType()).embedding(chatRequest);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
