package com.datacopilotx.aigateway.service.chat;

import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AIChatService {

    String chatCompletions(ChatRequest chatRequest);

    Flux<String> streamChatCompletions(ChatRequest chatRequest);

    List<Float> embedding(ChatRequest chatRequest);
}
