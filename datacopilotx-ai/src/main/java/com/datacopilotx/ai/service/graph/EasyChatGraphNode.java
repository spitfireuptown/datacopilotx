package com.datacopilotx.ai.service.graph;

import cn.hutool.core.lang.Pair;
import com.datacopilotx.ai.mapper.QuestionLogMapper;
import com.datacopilotx.ai.service.graph.main.WorkflowServiceHelper;
import com.datacopilotx.ai.service.graph.main.SerializableSink;
import com.datacopilotx.ai.service.graph.main.WorkflowState;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.common.constant.PromptConstant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class EasyChatGraphNode implements NodeAction<WorkflowState> {
    @Resource
    private AIGatewayChatService aiGatewayChatService;
    @Resource
    private WorkflowServiceHelper workflowServiceHelper;
    @Resource
    private QuestionLogMapper questionLogMapper;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        String beautifulQuestion = state.beautifulQuestion().orElseThrow(() -> new IllegalArgumentException("beautifulQuestion is empty"));

        Pair<String, String> promptPair = workflowServiceHelper.injectPrompt(
                PromptConstant.EASY_CHAT_PROMPT,
                Map.of("${query}", beautifulQuestion)
        );

        ChatRequest chatRequest = state.buildLLMRequest();
        chatRequest.setSystemPrompt(promptPair.getKey());
        chatRequest.setUserPrompt(promptPair.getValue());

        StringBuilder resultBuilder = new StringBuilder();
        Sinks.Many<org.springframework.http.codec.ServerSentEvent<com.datacopilotx.common.result.WebResult<String>>> sink = state.getSink();
        SerializableSink serializableSink = state.getSerializableSink();

        workflowServiceHelper.streamPrint(sink, PromptConstant.EASY_CHAT_NODE, "\n", serializableSink, state);
        workflowServiceHelper.streamPrint(sink, PromptConstant.EASY_CHAT_NODE, "\n", serializableSink, state);
        workflowServiceHelper.streamPrint(sink, PromptConstant.EASY_CHAT_NODE, "#### 回答: ", serializableSink, state);
        workflowServiceHelper.streamPrint(sink, PromptConstant.EASY_CHAT_NODE, "\n", serializableSink, state);

        aiGatewayChatService.streamChatCompletions(chatRequest)
                .doOnNext(chunk -> {
                    resultBuilder.append(chunk);
                    workflowServiceHelper.streamPrint(sink, PromptConstant.EASY_CHAT_NODE, chunk, serializableSink, state);
                })
                .doOnError(e -> log.error("流式输出异常: {}", e.getMessage()))
                .blockLast();

        String answer = resultBuilder.toString();

        Map<String, Object> collectedDataUpdate = state.appendCollectedData(answer);

        Map<String, Object> returnMap = new HashMap<>();
        returnMap.put("easy_chat_answer", answer);
        returnMap.put("answer", answer);
        returnMap.put("token", chatRequest.getTokenUsage());
        returnMap.put("time_cost", chatRequest.getTimeCost());
        returnMap.putAll(collectedDataUpdate);
        return returnMap;
    }
}
