package com.datacopilotx.ai.service.flow;

import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONUtil;
import com.datacopilotx.ai.controller.form.QuestionForm;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.common.constant.PromptConstant;
import com.datacopilotx.common.result.WebResult;
import com.datacopilotx.common.util.WorkflowUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;

/**
 * 意图识别
 */
@Slf4j
@Component
public class IntentRecognitionStep extends AbstractChatProcessStep {
    @Resource
    private AIGatewayChatService aiGatewayChatService;
    
    @Resource
    private WorkflowServiceHelper workflowServiceHelper;
    
    @Resource
    private EasyChatStep easyChatStep;
    
    @Override
    public void process(Sinks.Many<ServerSentEvent<WebResult<String>>> sink, Map<String, Object> preResultMap, QuestionForm questionForm) {
        Pair<String, String> promptPair = workflowServiceHelper.injectPrompt(
                PromptConstant.INTENT_RECOGNITION_PROMPT,
                Map.of(
                    "${query}", questionForm.getBeautifulQuestion(),
                    "${meta}", workflowServiceHelper.assembleDataSetInfo(questionForm.getDataSetBean())
                )
        );

        workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, "\n", questionForm);
        workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, "\n", questionForm);
        workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, "#### 意图识别: ", questionForm);
        workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, "\n", questionForm);
        workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, "\n", questionForm);

        ChatRequest chatRequest = questionForm.buildLLMRequest();
        chatRequest.setSystemPrompt(promptPair.getKey());
        chatRequest.setUserPrompt(promptPair.getValue());

        StringBuilder relationAnalysisResultBuilder = new StringBuilder();
        aiGatewayChatService.streamChatCompletions(chatRequest)
                .flatMap(response -> {
                    relationAnalysisResultBuilder.append(response);
                    return Mono.just(response);
                })
                .subscribe(
                        (data) -> {},
                        (error) -> workflowServiceHelper.errorHandling(PromptConstant.INTENT_RECOGNITION_NODE, sink, "相关性分析问题异常" + error.getMessage()),
                        () -> {
                            String relationAnalysisResult = WorkflowUtil.cleanJsonStr(relationAnalysisResultBuilder.toString());
                            try {
                                Map<String, Object> relationAnalysisResultMap = JSONUtil.toBean(relationAnalysisResult, Map.class);
                                Integer score = (Integer) relationAnalysisResultMap.get("score");

                                workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, "\n", questionForm);

                                String analysis = (String) relationAnalysisResultMap.get("analysis");
                                List<String> reasonSpilt = WorkflowUtil.splitString(analysis, 1);
                                for (String subReason : reasonSpilt) {
                                    workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, subReason, questionForm);
                                }

                                questionForm.setToken(questionForm.getToken() + chatRequest.getTokenUsage());
                                questionForm.setTimeCost(questionForm.getTimeCost() + chatRequest.getTimeCost());

                                if (score == 1) {
                                    // 继续执行下一个步骤（生成SQL）
                                    proceedToNextStep(sink, relationAnalysisResultMap, questionForm);
                                } else {
                                    // 执行闲聊步骤
                                    easyChatStep.process(sink, relationAnalysisResultMap, questionForm);
                                }
                            } catch (Exception e) {
                                log.error("相关性分析问题异常: {}", e.getMessage(), e);
                                workflowServiceHelper.streamPrint(sink, PromptConstant.INTENT_RECOGNITION_NODE, "\n", questionForm);
                                workflowServiceHelper.errorHandling(PromptConstant.INTENT_RECOGNITION_NODE, sink, "相关性分析问题异常 " + e.getMessage());
                            }
                        }
                );
    }
}