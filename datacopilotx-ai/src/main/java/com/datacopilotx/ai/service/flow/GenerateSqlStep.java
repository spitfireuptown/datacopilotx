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
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;

/**
 * 生成SQL步骤
 */
@Slf4j
@Component
public class GenerateSqlStep extends AbstractChatProcessStep {
    @Resource
    private AIGatewayChatService aiGatewayChatService;
    
    @Resource
    private WorkflowServiceHelper workflowServiceHelper;
    
    @Override
    public void process(Sinks.Many<ServerSentEvent<WebResult<String>>> sink, Map<String, Object> preResultMap, QuestionForm questionForm) {
        Pair<String, String> promptPair = workflowServiceHelper.injectPrompt(
                PromptConstant.SQL_GENERATION_PROMPT,
                Map.of(
                    "${time}", workflowServiceHelper.getCurrentTime(),
                    "${query}", questionForm.getBeautifulQuestion(),
                    "${engine}", questionForm.getModelConfigBean().getType(),
                    "${innerPrompt}", questionForm.getDataSetBean().getInjectPrompt(),
                    "${recall}", ObjectUtils.isEmpty(preResultMap.get("recall")) ? "" : JSONUtil.toJsonStr(preResultMap.get("recall")),
                    "${meta}", workflowServiceHelper.assembleDataSetInfo(questionForm.getDataSetBean())
                )
        );
        workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE, "\n", questionForm);
        workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE, "\n", questionForm);
        workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE, "#### SQL: ", questionForm);
        workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE, "\n", questionForm);

        ChatRequest chatRequest = questionForm.buildLLMRequest();
        chatRequest.setSystemPrompt(promptPair.getKey());
        chatRequest.setUserPrompt(promptPair.getValue());

        StringBuilder generateSqlResultBuilder = new StringBuilder();
        aiGatewayChatService.streamChatCompletions(chatRequest)
                .flatMap(response -> {
                    generateSqlResultBuilder.append(response);
                    return Mono.just(response);
                })
                .subscribe(
                        (data) -> workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE, data, questionForm),
                        (error) -> workflowServiceHelper.errorHandling(PromptConstant.SQL_GENERATION_NODE, sink, "SQL生成异常" + error.getMessage()),
                        () -> {
                            // 提取SQL生成结果
                            String generateSqlResultStr = workflowServiceHelper.extractContentAfterMarker(generateSqlResultBuilder.toString());
                            String generateSqlResult = WorkflowUtil.cleanJsonStr(generateSqlResultStr);
                            try {
                                Map<String, Object> generateSqlResultMap = JSONUtil.toBean(generateSqlResult, Map.class);
                                String sql = (String) generateSqlResultMap.get("sql");

                                // 执行SQL步骤
                                questionForm.setToken(questionForm.getToken() + chatRequest.getTokenUsage());
                                questionForm.setTimeCost(questionForm.getTimeCost() + chatRequest.getTimeCost());
                                questionForm.setSql(sql);
                                proceedToNextStep(sink, preResultMap, questionForm);
                            } catch (Exception e) {
                                log.error("SQL生成运行异常: {}", e.getMessage());
                                workflowServiceHelper.errorHandling(PromptConstant.SQL_GENERATION_NODE, sink, "SQL生成运行异常 " + e.getMessage());
                            }
                        }
                );
    }
}