package com.datacopilotx.ai.service.graph;

import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONUtil;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.mapper.QuestionLogMapper;
import com.datacopilotx.ai.service.graph.main.WorkflowServiceHelper;
import com.datacopilotx.ai.service.graph.main.SerializableSink;
import com.datacopilotx.ai.service.graph.main.WorkflowState;
import com.datacopilotx.ai.util.ExceptionUtil;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.common.constant.PromptConstant;
import com.datacopilotx.common.util.WorkflowUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Slf4j
@Component
public class IntentRecognitionGraphNode implements NodeAction<WorkflowState> {
    @Resource
    private AIGatewayChatService aiGatewayChatService;

    @Resource
    private WorkflowServiceHelper workflowServiceHelper;


    @Override
    public Map<String, Object> apply(WorkflowState state) {
        log.info("IntentRecognitionGraphNode apply");

        DataSetBean dataSetBean = state.getDataSetBean();
        String beautifulQuestion = state.beautifulQuestion().orElseThrow(() -> new IllegalArgumentException("beautifulQuestion is empty"));

        int currentRetryCount = state.retryCount().orElse(0);
        String intentError = state.intentAnalysis().orElse("");

        Pair<String, String> promptPair = workflowServiceHelper.injectPrompt(
                PromptConstant.INTENT_RECOGNITION_PROMPT,
                Map.of(
                        "${query}", beautifulQuestion,
                        "${meta}", workflowServiceHelper.assembleDataSetInfo(dataSetBean, beautifulQuestion, state)
                )
        );

        ChatRequest chatRequest = state.buildLLMRequest();
        chatRequest.setSystemPrompt(promptPair.getKey());
        chatRequest.setUserPrompt(promptPair.getValue());

        // 如果有上一次的错误信息，追加到user prompt中，让LLM知道需要修正
        if (!intentError.isEmpty()) {
            chatRequest.setUserPrompt(promptPair.getValue() + "\n\n【上一次输出错误，请修正】\n" + intentError);
        }

        StringBuilder resultBuilder = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        Sinks.Many<org.springframework.http.codec.ServerSentEvent<com.datacopilotx.common.result.WebResult<String>>> sink = state.getSink();
        SerializableSink serializableSink = state.getSerializableSink();

        workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, "\n", serializableSink, state);
        workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, "\n", serializableSink, state);
        workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, "#### 意图识别: ", serializableSink, state);
        workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, "\n", serializableSink, state);
        workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, "\n", serializableSink, state);

        aiGatewayChatService.streamChatCompletions(chatRequest)
                .doOnNext(resultBuilder::append)
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

        try {
            String relationAnalysisResult = WorkflowUtil.cleanJsonStr(resultBuilder.toString());
            if (relationAnalysisResult == null || relationAnalysisResult.trim().isEmpty()) {
                throw new RuntimeException("意图识别结果为空");
            }

            Map<String, Object> relationAnalysisResultMap = JSONUtil.toBean(relationAnalysisResult, Map.class);
            Integer score = (Integer) relationAnalysisResultMap.get("score");
            String analysis = (String) relationAnalysisResultMap.get("analysis");

            if (score == null) {
                throw new RuntimeException("意图识别结果中缺少score字段");
            }

            workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, "\n", serializableSink, state);

            if (analysis != null) {
                List<String> reasonSpilt = WorkflowUtil.splitString(analysis, 1);
                for (String subReason : reasonSpilt) {
                    workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, subReason, serializableSink, state);
                }
                state.appendCollectedData(analysis);
            }

            return Map.of(
                    "intent_score", score,
                    "intent_analysis", analysis != null ? analysis : "",
                    "answer", analysis != null ? analysis : "",
                    "token", chatRequest.getTokenUsage() != null ? chatRequest.getTokenUsage() : 0,
                    "time_cost", chatRequest.getTimeCost() != null ? chatRequest.getTimeCost() : 0
            );
        } catch (Exception e) {
            log.error("意图识别失败: {}", e.getMessage(), e);
            int retryCount = currentRetryCount + 1;

            workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE,
                    "\n模型繁忙，正在重试 " + retryCount + "/3...\n", serializableSink, state);

            String newIntentError = "意图识别失败: " + ExceptionUtil.getFullStackTrace(e) + "\n请确保输出标准的JSON格式，包含score和analysis字段。";
            if (!intentError.isEmpty()) {
                newIntentError = intentError + "\n" + newIntentError;
            }

            return Map.of(
                    "intent_analysis", newIntentError,
                    "retry_count", retryCount,
                    "intent_score", -1,
                    "answer", ""
            );
        }
    }
}
