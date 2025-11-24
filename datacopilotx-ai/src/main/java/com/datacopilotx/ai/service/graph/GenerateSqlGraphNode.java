package com.datacopilotx.ai.service.graph;

import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONUtil;
import com.datacopilotx.ai.controller.form.QuestionForm;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.ModelConfigBean;
import com.datacopilotx.ai.service.flow.AbstractChatProcessStep;
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

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 生成SQL步骤
 */
@Slf4j
@Component
public class GenerateSqlGraphNode implements NodeAction<WorkflowState> {
    @Resource
    private AIGatewayChatService aiGatewayChatService;
    
    @Resource
    private WorkflowServiceHelper workflowServiceHelper;

    @Override
    public Map<String, Object> apply(WorkflowState workflowState) throws Exception {
        Sinks.Many<ServerSentEvent<WebResult<String>>> sink = workflowState.sink().orElseThrow(() -> new DataCopilotXException("sink is empty"));

        String beautifulQuestion = workflowState.beautifulQuestion().orElseThrow(() ->new DataCopilotXException("question_form is null"));
        ModelConfigBean modelConfigBean = workflowState.modelConfigBean().orElseThrow(() ->new DataCopilotXException("model_config_bean is null"));
        DataSetBean dataSetBean = workflowState.dataSetBean().orElseThrow(() ->new DataCopilotXException("data_set_bean is null"));

        Pair<String, String> promptPair = workflowServiceHelper.injectPrompt(
                PromptConstant.SQL_GENERATION_PROMPT,
                Map.of(
                        "${time}", workflowServiceHelper.getCurrentTime(),
                        "${query}", beautifulQuestion,
                        "${engine}", modelConfigBean.getType(),
                        "${innerPrompt}", dataSetBean.getInjectPrompt(),
//                        "${recall}", JSONUtil.toJsonStr(workflowState.getResult("recall")),
                        "${meta}", workflowServiceHelper.assembleDataSetInfo(dataSetBean)
                )
        );
        workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE, "\n");
        workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE, "\n");
        workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE, "#### SQL: ");
        workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE, "\n");

        ChatRequest chatRequest = workflowState.buildLLMRequest();
        chatRequest.setSystemPrompt(promptPair.getKey());
        chatRequest.setUserPrompt(promptPair.getValue());

        StringBuilder generateSqlResultBuilder = new StringBuilder();
        
        // 添加同步机制
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Map<String, Object>> resultMapRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        AtomicReference<String> sqlRef = new AtomicReference<>();
        
        aiGatewayChatService.streamChatCompletions(chatRequest)
                .flatMap(response -> {
                    generateSqlResultBuilder.append(response);
                    return Mono.just(response);
                })
                .subscribe(
                        (data) -> workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE, data),
                        (error) -> {
                            workflowServiceHelper.errorHandling(PromptConstant.SQL_GENERATION_NODE, sink, "SQL生成异常" + error.getMessage());
                            errorRef.set(error);
                            latch.countDown();
                        },
                        () -> {
                            String generateSqlResult = WorkflowUtil.cleanJsonStr(generateSqlResultBuilder.toString());
                            try {
                                Map<String, Object> generateSqlResultMap = JSONUtil.toBean(generateSqlResult, Map.class);
                                String sql = (String) generateSqlResultMap.get("sql");
                                sqlRef.set(sql);

                                // 将结果存储到AtomicReference中
                                resultMapRef.set(Map.of(
                                        "sql", sql,
                                        "token", chatRequest.getTokenUsage(),
                                        "time_cost", chatRequest.getTimeCost()
                                ));
                            } catch (Exception e) {
                                log.error("SQL生成运行异常: {}", e.getMessage());
                                workflowServiceHelper.errorHandling(PromptConstant.SQL_GENERATION_NODE, sink, "SQL生成运行异常 " + e.getMessage());
                                errorRef.set(e);
                            } finally {
                                latch.countDown(); // 无论成功失败都释放锁
                            }
                        }
                );
        
        // 等待异步操作完成
        latch.await();
        
        // 检查是否有错误发生
        if (errorRef.get() != null) {
            throw new DataCopilotXException("SQL生成过程中发生错误: " + errorRef.get().getMessage());
        }
        
        // 返回结果
        return resultMapRef.get() != null ? resultMapRef.get() : Map.of();
    }
}