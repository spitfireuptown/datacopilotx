package com.datacopilotx.ai.service.graph;

import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONUtil;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.ModelConfigBean;
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
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        int currentRetryCount = state.retryCount().orElse(0);

        if (currentRetryCount >= 3) {
            log.error("SQL生成已达到最大重试次数: 3");
            return Map.of(
                    "sql", "",
                    "token", 0,
                    "time_cost", 0,
                    "sql_error", "SQL生成失败，已达到最大重试次数"
            );
        }

        HashMap<String, String> promptParams = new HashMap<>();
        promptParams.put("${time}", workflowServiceHelper.getCurrentTime());
        promptParams.put("${query}", beautifulQuestion);
        promptParams.put("${engine}", modelConfigBean.getType());
        promptParams.put("${innerPrompt}", "");
        promptParams.put("${recall}", ObjectUtils.isEmpty(recall) ? "" : JSONUtil.toJsonStr(recall));
        promptParams.put("${meta}", workflowServiceHelper.assembleDataSetInfo(dataSetBean, beautifulQuestion, state));
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
        Sinks.Many<org.springframework.http.codec.ServerSentEvent<com.datacopilotx.common.result.WebResult<String>>> sink = state.getSink();
        SerializableSink serializableSink = state.getSerializableSink();

        workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE, "\n", serializableSink, state);
        workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE, "\n", serializableSink, state);
        workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE, "#### SQL: ", serializableSink, state);
        workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE, "\n", serializableSink, state);

        try {
            aiGatewayChatService.streamChatCompletions(chatRequest)
                    .doOnNext(chunk -> {
                        resultBuilder.append(chunk);
                        workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE, chunk, serializableSink, state);
                    })
                    .doOnError(e -> log.error("流式输出异常: {}", e.getMessage()))
                    .blockLast();

            String generateSqlResult = WorkflowUtil.cleanJsonStr(resultBuilder.toString());
            
            if (generateSqlResult == null || generateSqlResult.trim().isEmpty()) {
                throw new RuntimeException("流式输出结果为空");
            }
            
            Map<String, Object> generateSqlResultMap = JSONUtil.toBean(generateSqlResult, Map.class);
            String sql = (String) generateSqlResultMap.get("sql");

            if (sql == null || sql.trim().isEmpty()) {
                throw new RuntimeException("生成的SQL为空，请重新生成");
            }

            Map<String, Object> collectedDataUpdate = state.appendCollectedData(sql);
            
            log.info("Generated SQL: {}", sql);
            Map<String, Object> result = new HashMap<>();
            result.put("sql", sql);
            result.put("token", chatRequest.getTokenUsage() != null ? chatRequest.getTokenUsage() : 0);
            result.put("time_cost", chatRequest.getTimeCost() != null ? chatRequest.getTimeCost() : 0);
            result.put("sql_error", "");
            result.putAll(collectedDataUpdate);
            return result;
        } catch (Exception e) {
            log.error("SQL生成失败: {}", e.getMessage(), e);
            int retryCount = currentRetryCount + 1;

            workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_GENERATION_NODE,
                    "\nSQL生成失败，正在重试 " + retryCount + "/3...\n", serializableSink, state);

            String newSqlError = "SQL生成失败: " + ExceptionUtil.getFullStackTrace(e) + "\n请根据错误信息修复并重新生成SQL。";
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
