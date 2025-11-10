package com.datacopilotx.aigateway.service.chat;


import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.common.constant.GlobalConstant;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AIGatewayChatService implements AIChatService, ApplicationContextAware {

    public Map<String, AIChatService> messageHubServiceMap = new ConcurrentHashMap<>();
    private ApplicationContext applicationContext;

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
        return this.aiModelFactory(chatRequest.getType()).streamChatCompletions(chatRequest);
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
