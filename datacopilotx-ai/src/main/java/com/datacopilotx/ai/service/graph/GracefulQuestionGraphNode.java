package com.datacopilotx.ai.service.graph;

import cn.hutool.core.lang.Pair;
import com.datacopilotx.ai.service.graph.main.WorkflowServiceHelper;
import com.datacopilotx.ai.service.graph.main.WorkflowState;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.common.constant.PromptConstant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
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
        var serializableSink = state.getSerializableSink();

        workflowServiceHelper.streamPrint(sink, PromptConstant.BEAUTIFUL_NODE, "#### 用户问题: ", serializableSink, state);
        workflowServiceHelper.streamPrint(sink, PromptConstant.BEAUTIFUL_NODE, "\n", serializableSink, state);

        aiGatewayChatService.streamChatCompletions(chatRequest)
                .doOnNext(chunk -> {
                    resultBuilder.append(chunk);
                    workflowServiceHelper.streamPrint(sink, PromptConstant.BEAUTIFUL_NODE, chunk, serializableSink, state);
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
        
        // 将收集到的流式数据追加到WorkflowState
        state.appendCollectedData(result);

        return Map.of(
                "beautiful_question", result,
                "answer", result,
                "token", chatRequest.getTokenUsage()
        );
    }
}
