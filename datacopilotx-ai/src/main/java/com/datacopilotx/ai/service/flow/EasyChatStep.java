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
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;

/**
 * 闲聊步骤
 */
@Slf4j
@Component
public class EasyChatStep extends AbstractChatProcessStep {
    @Resource
    private AIGatewayChatService aiGatewayChatService;
    
    @Resource
    private WorkflowServiceHelper workflowServiceHelper;

    /**
     * 进入到闲聊即最后一个节点
     */
    @Override
    public void process(Sinks.Many<ServerSentEvent<WebResult<String>>> sink, Map<String, Object> preResultMap, QuestionForm questionForm) {
        Pair<String, String> promptPair = workflowServiceHelper.injectPrompt(
                PromptConstant.EASY_CHAT_PROMPT,
                Map.of(
                        "${query}", questionForm.getBeautifulQuestion()
                )
        );

        workflowServiceHelper.streamPrint(sink, PromptConstant.EASY_CHAT_NODE, "\n", questionForm);
        workflowServiceHelper.streamPrint(sink, PromptConstant.EASY_CHAT_NODE, "\n", questionForm);
        workflowServiceHelper.streamPrint(sink, PromptConstant.EASY_CHAT_NODE, "#### 回答: ", questionForm);
        workflowServiceHelper.streamPrint(sink, PromptConstant.EASY_CHAT_NODE, "\n", questionForm);

        ChatRequest chatRequest = questionForm.buildLLMRequest();
        chatRequest.setSystemPrompt(promptPair.getKey());
        chatRequest.setUserPrompt(promptPair.getValue());

        aiGatewayChatService.streamChatCompletions(chatRequest)
                .flatMap(Mono::just)
                .subscribe(
                        (data) -> workflowServiceHelper.streamPrint(sink, PromptConstant.EASY_CHAT_NODE, data, questionForm),
                        (error) -> workflowServiceHelper.errorHandling(PromptConstant.EASY_CHAT_NODE, sink, "闲聊异常" + error.getMessage()),
                        () -> {
                            try {
                                questionForm.setToken(questionForm.getToken() + chatRequest.getTokenUsage());
                                questionForm.setTimeCost(questionForm.getTimeCost() + chatRequest.getTimeCost());
                                proceedToNextStep(sink, preResultMap, questionForm);
                                sink.tryEmitComplete();
                            } catch (Exception e) {
                                log.error("闲聊运行异常: {}", e.getMessage());
                                workflowServiceHelper.errorHandling(PromptConstant.EASY_CHAT_NODE, sink, "闲聊运行异常 " + e.getMessage());
                            }
                        }
                );
    }
}