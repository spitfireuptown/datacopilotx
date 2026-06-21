package com.datacopilotx.ai.service.graph;

import cn.hutool.core.lang.Pair;
import com.datacopilotx.ai.service.flow.WorkflowServiceHelper;
import com.datacopilotx.ai.service.graph.main.WorkflowState;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.common.constant.PromptConstant;
import com.datacopilotx.common.result.WebResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Slf4j
@Component
public class GracefulQuestionGraphNode implements NodeAction<WorkflowState> {

    @Resource
    private AIGatewayChatService aiGatewayChatService;

    @Resource
    private WorkflowServiceHelper workflowServiceHelper;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        String questionId = state.questionId().orElseThrow(() -> new IllegalArgumentException("questionId is empty"));
        String question = state.question().orElseThrow(() -> new IllegalArgumentException("question is empty"));

        Pair<String, String> promptPair = workflowServiceHelper.injectPrompt(
                PromptConstant.BEAUTIFUL_PROMPT,
                Map.of("${query}", question)
        );

        log.info("question_id: {}, beautifulQuestion: {}", questionId, promptPair);
        ChatRequest chatRequest = state.buildLLMRequest(question);
        chatRequest.setSystemPrompt(promptPair.getKey());
        chatRequest.setUserPrompt(promptPair.getValue());

        StringBuilder resultBuilder = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        var sink = state.getSink();

        aiGatewayChatService.streamChatCompletions(chatRequest)
                .doOnNext(chunk -> {
                    resultBuilder.append(chunk);
                    if (sink != null) {
                        sink.tryEmitNext(ServerSentEvent.<WebResult<String>>builder()
                                .event("node_progress")
                                .data(WebResult.success(chunk))
                                .build());
                    }
                })
                .doOnComplete(latch::countDown)
                .doOnError(e -> {
                    log.error("流式输出异常: {}", e.getMessage());
                    latch.countDown();
                })
                .subscribe();

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("等待流式输出完成被中断", e);
        }

        String result = resultBuilder.toString();
        if (sink != null) {
            sink.tryEmitNext(ServerSentEvent.<WebResult<String>>builder()
                    .event("node_progress")
                    .data(WebResult.success("问题美化完成"))
                    .build());
        }

        return Map.of(
                "beautiful_question", result,
                "answer", result,
                "token", chatRequest.getTokenUsage()
        );
    }
}
