package com.datacopilotx.ai.service.graph;

import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.QuestionLogBean;
import com.datacopilotx.ai.mapper.QuestionLogMapper;
import com.datacopilotx.ai.service.flow.WorkflowServiceHelper;
import com.datacopilotx.ai.service.graph.main.WorkflowState;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.common.constant.PromptConstant;
import com.datacopilotx.common.result.WebResult;
import com.datacopilotx.common.util.WorkflowUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;

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
        String questionId = state.questionId().orElseThrow(() -> new IllegalArgumentException("questionId is empty"));
        String sessionId = state.sessionId().orElseThrow(() -> new IllegalArgumentException("sessionId is empty"));
        String beautifulQuestion = state.beautifulQuestion().orElseThrow(() -> new IllegalArgumentException("beautifulQuestion is empty"));


        Pair<String, String> promptPair = workflowServiceHelper.injectPrompt(
                PromptConstant.INTENT_RECOGNITION_PROMPT,
                Map.of(
                        "${query}", beautifulQuestion,
                        "${meta}", workflowServiceHelper.assembleDataSetInfo(dataSetBean)
                )
        );

        ChatRequest chatRequest = state.buildLLMRequest();
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

        String relationAnalysisResult = WorkflowUtil.cleanJsonStr(resultBuilder.toString());
        Map<String, Object> relationAnalysisResultMap = JSONUtil.toBean(relationAnalysisResult, Map.class);
        Integer score = (Integer) relationAnalysisResultMap.get("score");
        String analysis = (String) relationAnalysisResultMap.get("analysis");

        QuestionLogBean questionLogBean = questionLogMapper.selectOne(new LambdaQueryWrapper<QuestionLogBean>()
                .eq(QuestionLogBean::getQuestionId, questionId)
                .eq(QuestionLogBean::getSessionId, sessionId)
        );
        if (questionLogBean != null && analysis != null) {
            questionLogBean.setAnswer(questionLogBean.getAnswer() + analysis);
            questionLogMapper.updateById(questionLogBean);
        }

        return Map.of(
                "intent_score", score,
                "intent_analysis", analysis,
                "answer", analysis,
                "token", chatRequest.getTokenUsage()
        );
    }
}
