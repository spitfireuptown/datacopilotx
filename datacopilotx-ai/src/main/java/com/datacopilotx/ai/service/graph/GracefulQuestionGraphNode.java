package com.datacopilotx.ai.service.graph;

import cn.hutool.core.lang.Pair;
import com.datacopilotx.ai.service.flow.WorkflowServiceHelper;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.common.constant.PromptConstant;
import com.datacopilotx.common.exception.DataCopilotXException;
import com.datacopilotx.common.result.WebResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * 美化问题步骤
 */
@Slf4j
@Component
public class GracefulQuestionGraphNode implements NodeAction<WorkflowState> {

    @Resource
    private AIGatewayChatService aiGatewayChatService;
    
    @Resource
    private WorkflowServiceHelper workflowServiceHelper;
    
    @Override
    public Map<String, Object> apply(WorkflowState workflowState) throws Exception {
        String questionId = workflowState.questionId().orElse("");

        String question = workflowState.question().orElse("");
        Sinks.Many<ServerSentEvent<WebResult<String>>> sink = workflowState.sink().orElseThrow(() -> new DataCopilotXException("sink is empty"));

        Pair<String, String> promptPair = workflowServiceHelper.injectPrompt(
                PromptConstant.BEAUTIFUL_PROMPT,
                Map.of("${query}", question)
        );

        workflowServiceHelper.streamPrint(sink, PromptConstant.BEAUTIFUL_NODE, "#### 用户问题: ");
        workflowServiceHelper.streamPrint(sink, PromptConstant.BEAUTIFUL_NODE, "\n");

        log.info("question_id: {}, beautifulQuestion: {}", questionId, promptPair);
        ChatRequest chatRequest = workflowState.buildLLMRequest();
        chatRequest.setSystemPrompt(promptPair.getKey());
        chatRequest.setUserPrompt(promptPair.getValue());

        StringBuilder result = new StringBuilder();
        Map<String, Object> returnMap = new HashMap<>();
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        aiGatewayChatService.streamChatCompletions(chatRequest)
                .flatMap(response -> {
                    result.append(response);
                    return Mono.just(response);
                })
                .subscribe(
                        (data) -> workflowServiceHelper.streamPrint(sink, PromptConstant.BEAUTIFUL_NODE, data),
                        (error) -> {
                            workflowServiceHelper.errorHandling(PromptConstant.BEAUTIFUL_NODE, sink, "美化问题异常" + error.getMessage());
                            future.completeExceptionally(error);
                        },
                        () -> {
                            // 将结果设置到返回Map中
                            returnMap.put("beautiful_question", result.toString());
                            returnMap.put("token", chatRequest.getTokenUsage());
                            returnMap.put("time_cost", chatRequest.getTimeCost());
                            future.complete(null);
                        }
                );

        // 同步等待异步操作完成
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage(), e);
            throw new DataCopilotXException("美化问题处理失败");
        }
        
        return returnMap;
    }
}