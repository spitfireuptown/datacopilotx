package com.datacopilotx.ai.service.flow;

import cn.hutool.core.lang.Pair;
import com.datacopilotx.ai.controller.form.QuestionForm;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.common.constant.PromptConstant;
import com.datacopilotx.common.result.WebResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;

/**
 * 美化问题步骤
 */
@Slf4j
@Component
public class GracefulQuestionStep extends AbstractChatProcessStep {

    @Resource
    private AIGatewayChatService aiGatewayChatService;
    
    @Resource
    private WorkflowServiceHelper workflowServiceHelper;
    
    @Override
    public void process(Sinks.Many<ServerSentEvent<WebResult<String>>> sink, Map<String, Object> preResultMap, QuestionForm questionForm) {
        Pair<String, String> promptPair = workflowServiceHelper.injectPrompt(
                PromptConstant.BEAUTIFUL_PROMPT,
                Map.of("${query}", questionForm.getQuestion())
        );

        workflowServiceHelper.streamPrint(sink, PromptConstant.BEAUTIFUL_NODE, "#### 用户问题: ", questionForm);
        workflowServiceHelper.streamPrint(sink, PromptConstant.BEAUTIFUL_NODE, "\n", questionForm);

        log.info("question_id: {}, beautifulQuestion: {}", questionForm.getQuestionId(), promptPair);
        ChatRequest chatRequest = questionForm.buildLLMRequest();
        chatRequest.setSystemPrompt(promptPair.getKey());
        chatRequest.setUserPrompt(promptPair.getValue());

        StringBuilder result = new StringBuilder();
        aiGatewayChatService.streamChatCompletions(chatRequest)
            .flatMap(response -> {
                result.append(response);
                return Mono.just(response);
            })
            .subscribe(
                (data) -> workflowServiceHelper.streamPrint(sink, PromptConstant.BEAUTIFUL_NODE, data, questionForm),
                (error) -> workflowServiceHelper.errorHandling(PromptConstant.BEAUTIFUL_NODE, sink, "美化问题异常" + error.getMessage()),
                () -> {
                    questionForm.setBeautifulQuestion(workflowServiceHelper.extractContentAfterMarker(result.toString()));
                    questionForm.setToken(chatRequest.getTokenUsage());
                    questionForm.setTimeCost(chatRequest.getTimeCost());
                    try {
                        // 继续执行下一个步骤
                        proceedToNextStep(sink, preResultMap, questionForm);
                    } catch (Exception e) {
                        log.error("美化运行异常: ", e);
                        workflowServiceHelper.errorHandling(PromptConstant.BEAUTIFUL_NODE, sink, "美化运行异常 " + e.getMessage());
                    }
                }
            );
    }
}