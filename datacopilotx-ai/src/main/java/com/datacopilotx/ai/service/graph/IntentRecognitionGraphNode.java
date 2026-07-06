package com.datacopilotx.ai.service.graph;

import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONUtil;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.mapper.QuestionLogMapper;
import com.datacopilotx.ai.service.graph.main.WorkflowServiceHelper;
import com.datacopilotx.ai.service.graph.main.SerializableSink;
import com.datacopilotx.ai.service.graph.main.WorkflowState;
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

    @Resource
    private QuestionLogMapper questionLogMapper;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        log.info("IntentRecognitionGraphNode apply");

        DataSetBean dataSetBean = state.getDataSetBean();
        String beautifulQuestion = state.beautifulQuestion().orElseThrow(() -> new IllegalArgumentException("beautifulQuestion is empty"));


        Pair<String, String> promptPair = workflowServiceHelper.injectPrompt(
                PromptConstant.INTENT_RECOGNITION_PROMPT,
                Map.of(
                        "${query}", beautifulQuestion,
                        "${meta}", workflowServiceHelper.assembleDataSetInfo(dataSetBean, beautifulQuestion)
                )
        );

        ChatRequest chatRequest = state.buildLLMRequest();
        chatRequest.setSystemPrompt(promptPair.getKey());
        chatRequest.setUserPrompt(promptPair.getValue());

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
                .doOnNext(chunk -> {
                    resultBuilder.append(chunk);
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

        String relationAnalysisResult = WorkflowUtil.cleanJsonStr(resultBuilder.toString());
        Map<String, Object> relationAnalysisResultMap = JSONUtil.toBean(relationAnalysisResult, Map.class);
        Integer score = (Integer) relationAnalysisResultMap.get("score");
        String analysis = (String) relationAnalysisResultMap.get("analysis");

        workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, "\n", serializableSink, state);

        List<String> reasonSpilt = WorkflowUtil.splitString(analysis, 1);
        for (String subReason : reasonSpilt) {
            workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, subReason, serializableSink, state);
        }

        // 将分析结果追加到WorkflowState
        if (analysis != null) {
            state.appendCollectedData(analysis);
        }

        return Map.of(
                "intent_score", score,
                "intent_analysis", analysis,
                "answer", analysis,
                "token", chatRequest.getTokenUsage()
        );
    }
}
