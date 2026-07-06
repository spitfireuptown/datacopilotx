package com.datacopilotx.ai.service.graph;

import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONUtil;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.ModelConfigBean;
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
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Slf4j
@Component
public class GenerateSqlGraphNode implements NodeAction<WorkflowState> {
    @Resource
    private AIGatewayChatService aiGatewayChatService;

    @Resource
    private WorkflowServiceHelper workflowServiceHelper;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        String beautifulQuestion = state.beautifulQuestion().orElseThrow(() -> new IllegalArgumentException("beautifulQuestion is null"));
        ModelConfigBean modelConfigBean = state.getModelConfigBean();
        DataSetBean dataSetBean = state.getDataSetBean();

        List<String> recall = state.recall();
        String sqlError = state.sqlError().orElse("");
        String intentAnalysis = state.intentAnalysis().orElse("");

        HashMap<String, String> promptParams = new HashMap<>();
        promptParams.put("${time}", workflowServiceHelper.getCurrentTime());
        promptParams.put("${query}", beautifulQuestion);
        promptParams.put("${engine}", modelConfigBean.getType());
        promptParams.put("${innerPrompt}", "");
        promptParams.put("${recall}", ObjectUtils.isEmpty(recall) ? "" : JSONUtil.toJsonStr(recall));
        promptParams.put("${meta}", workflowServiceHelper.assembleDataSetInfo(dataSetBean, beautifulQuestion));
        promptParams.put("${analysis}", intentAnalysis);
        promptParams.put("${sql_error}", sqlError);

        Pair<String, String> promptPair = workflowServiceHelper.injectPrompt(
                PromptConstant.SQL_GENERATION_PROMPT,
                promptParams
        );

        ChatRequest chatRequest = state.buildLLMRequest();
        chatRequest.setSystemPrompt(promptPair.getKey());
        chatRequest.setUserPrompt(promptPair.getValue());

        StringBuilder resultBuilder = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        Sinks.Many<org.springframework.http.codec.ServerSentEvent<com.datacopilotx.common.result.WebResult<String>>> sink = state.getSink();
        SerializableSink serializableSink = state.getSerializableSink();

        workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE, "\n", serializableSink, state);
        workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE, "\n", serializableSink, state);
        workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE, "#### SQL: ", serializableSink, state);
        workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE, "\n", serializableSink, state);

        aiGatewayChatService.streamChatCompletions(chatRequest)
                .doOnNext(chunk -> {
                    resultBuilder.append(chunk);
                    workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE, chunk, serializableSink, state);
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

        try {
            String generateSqlResult = WorkflowUtil.cleanJsonStr(resultBuilder.toString());
            Map<String, Object> generateSqlResultMap = JSONUtil.toBean(generateSqlResult, Map.class);
            String sql = (String) generateSqlResultMap.get("sql");

            if (sql == null || sql.trim().isEmpty()) {
                throw new RuntimeException("生成的SQL为空，请重新生成");
            }

            state.appendCollectedData(sql);
            
            log.info("Generated SQL: {}", sql);
            return Map.of(
                    "sql", sql,
                    "token", chatRequest.getTokenUsage(),
                    "sql_error", ""
            );
        } catch (Exception e) {
            log.error("SQL生成失败: {}", e.getMessage(), e);
            int retryCount = state.retryCount().orElse(0) + 1;

            workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE,
                    "\nSQL生成失败，正在重试...\n", serializableSink, state);

            String newSqlError = "SQL生成失败: " + e.getMessage() + "\n请根据错误信息修复并重新生成SQL。";
            if (!sqlError.isEmpty()) {
                newSqlError = sqlError + "\n" + newSqlError;
            }

            return Map.of(
                    "sql_error", newSqlError,
                    "retry_count", retryCount,
                    "sql", ""
            );
        }
    }
}
