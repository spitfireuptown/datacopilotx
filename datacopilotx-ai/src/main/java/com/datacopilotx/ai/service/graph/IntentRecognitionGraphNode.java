package com.datacopilotx.ai.service.graph;

import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.QuestionLogBean;
import com.datacopilotx.ai.mapper.QuestionLogMapper;
import com.datacopilotx.ai.service.flow.WorkflowServiceHelper;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.common.constant.PromptConstant;
import com.datacopilotx.common.exception.DataCopilotXException;
import com.datacopilotx.common.result.WebResult;
import com.datacopilotx.common.util.WorkflowUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 意图识别
 */
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
    public Map<String, Object> apply(WorkflowState workflowState) throws Exception {
        log.info("IntentRecognitionGraphNode apply");

        Sinks.Many<ServerSentEvent<WebResult<String>>> sink = workflowState.sink().orElseThrow(() -> new DataCopilotXException("sink is empty"));
        DataSetBean dataSetBean = workflowState.dataSetBean().orElseThrow(() -> new DataCopilotXException("dataSetBean is empty"));
        String questionId = workflowState.questionId().orElseThrow(() -> new DataCopilotXException("questionId is empty"));
        String sessionId = workflowState.sessionId().orElseThrow(() -> new DataCopilotXException("sessionId is empty"));
        String beautifulQuestion = workflowState.beautifulQuestion().orElse("");

        Pair<String, String> promptPair = workflowServiceHelper.injectPrompt(
                PromptConstant.INTENT_RECOGNITION_PROMPT,
                Map.of(
                        "${query}", beautifulQuestion,
                        "${meta}", workflowServiceHelper.assembleDataSetInfo(dataSetBean)
                )
        );

        workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, "\n");
        workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, "\n");
        workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, "#### 意图识别: ");
        workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, "\n");
        workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, "\n");

        ChatRequest chatRequest = workflowState.buildLLMRequest();
        chatRequest.setSystemPrompt(promptPair.getKey());
        chatRequest.setUserPrompt(promptPair.getValue());

        StringBuilder relationAnalysisResultBuilder = new StringBuilder();
        
        // 添加同步机制
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Map<String, Object>> resultMapRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        AtomicReference<String> analysis = null;

        aiGatewayChatService.streamChatCompletions(chatRequest)
                .flatMap(response -> {
                    relationAnalysisResultBuilder.append(response);
                    return Mono.just(response);
                })
                .subscribe(
                        (data) -> {},
                        (error) -> {
                            workflowServiceHelper.errorHandling(PromptConstant.INTENT_RECOGNITION_NODE, sink, "相关性分析问题异常" + error.getMessage());
                            errorRef.set(error);
                            latch.countDown();
                        },
                        () -> {
                            String relationAnalysisResult = WorkflowUtil.cleanJsonStr(relationAnalysisResultBuilder.toString());
                            try {
                                Map<String, Object> relationAnalysisResultMap = JSONUtil.toBean(relationAnalysisResult, Map.class);
                                Integer score = (Integer) relationAnalysisResultMap.get("score");

                                workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, "\n");

                                analysis.set((String) relationAnalysisResultMap.get("analysis"));
                                List<String> reasonSpilt = WorkflowUtil.splitString(analysis.get(), 1);
                                for (String subReason : reasonSpilt) {
                                    workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, subReason);
                                }

                                // 将结果存储到AtomicReference中
                                resultMapRef.set(Map.of(
                                        "intent_score", score,
                                        "intent_analysis", relationAnalysisResultMap.get("analysis"),
                                        "token", chatRequest.getTokenUsage(),
                                        "time_cost", chatRequest.getTimeCost()
                                ));
                            } catch (Exception e) {
                                log.error("相关性分析问题异常: {}", e.getMessage(), e);
                                workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, "\n");
                                workflowServiceHelper.errorHandling(PromptConstant.INTENT_RECOGNITION_NODE, sink, "相关性分析问题异常 " + e.getMessage());
                                errorRef.set(e);
                            } finally {
                                latch.countDown(); // 无论成功失败都释放锁
                            }
                        }
                );
        
        // 等待异步操作完成
        latch.await();

        QuestionLogBean questionLogBean = questionLogMapper.selectOne(new LambdaQueryWrapper<QuestionLogBean>()
                .eq(QuestionLogBean::getQuestionId, questionId)
                .eq(QuestionLogBean::getSessionId, sessionId)
        );
        questionLogBean.setAnswer(questionLogBean.getAnswer() + analysis.get());
        questionLogMapper.updateById(questionLogBean);
        
        // 检查是否有错误发生
        if (errorRef.get() != null) {
            throw new DataCopilotXException("意图识别过程中发生错误: " + errorRef.get().getMessage());
        }
        
        // 返回结果
        return resultMapRef.get() != null ? resultMapRef.get() : Map.of();
    }
}