package com.datacopilotx.ai.service.graph;

import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONUtil;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.ModelConfigBean;
import com.datacopilotx.ai.domian.dto.PermissionDTO;
import com.datacopilotx.ai.service.PermissionService;
import com.datacopilotx.ai.service.graph.main.WorkflowGraph;
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

import java.util.*;

@Slf4j
@Component
public class GenerateSqlGraphNode implements NodeAction<WorkflowState> {
    @Resource
    private AIGatewayChatService aiGatewayChatService;

    @Resource
    private WorkflowServiceHelper workflowServiceHelper;

    @Resource
    private PermissionService permissionService;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        String beautifulQuestion = state.beautifulQuestion().orElseThrow(() -> new IllegalArgumentException("beautifulQuestion is null"));
        ModelConfigBean modelConfigBean = state.getModelConfigBean();
        DataSetBean dataSetBean = state.getDataSetBean();

        List<String> recall = state.recall();
        String sqlError = state.sqlError().orElse("");
        String intentAnalysis = state.intentAnalysis().orElse("");
        int currentRetryCount = state.retryCount().orElse(0);

        String currentUserId = state.userId().orElse(null);
        Integer currentUserRole = state.userRole().orElse(2);
        boolean isAdmin = state.isAdmin().orElse(false);

        if (currentRetryCount >= WorkflowGraph.MAX_RETRY) {
            log.error("SQL生成已达到最大重试次数: {}，最后一次错误: {}", WorkflowGraph.MAX_RETRY, sqlError);
            String fallbackSql = isAdmin ? "" : "SELECT 1 WHERE 1=0";
            return Map.of(
                    "sql", fallbackSql,
                    "token", 0,
                    "time_cost", 0,
                    "sql_error", "SQL生成失败，已达到最大重试次数"
            );
        }

        String permissionRules = buildPermissionRules(dataSetBean.getId(), currentUserId, isAdmin);

        HashMap<String, String> promptParams = new HashMap<>();
        promptParams.put("${time}", workflowServiceHelper.getCurrentTime());
        promptParams.put("${query}", beautifulQuestion);
        promptParams.put("${engine}", modelConfigBean.getType());
        promptParams.put("${innerPrompt}", "");
        promptParams.put("${recall}", ObjectUtils.isEmpty(recall) ? "" : JSONUtil.toJsonStr(recall));
        promptParams.put("${meta}", workflowServiceHelper.assembleDataSetInfo(dataSetBean, beautifulQuestion, state));
        promptParams.put("${analysis}", intentAnalysis);
        promptParams.put("${sql_error}", sqlError);
        promptParams.put("${permission_rules}", permissionRules);

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

            Map<String, Object> collectedDataUpdate = state.appendCollectedData("\n\n#### SQL: \n" + sql);
            
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
                    "\nSQL生成失败，正在重试 " + retryCount + "/" + WorkflowGraph.MAX_RETRY + "...\n", serializableSink, state);

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

    private String buildPermissionRules(Long dsId, String userId, boolean isAdmin) {
        if (isAdmin) {
            return "";
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("user_id", userId);

        String columnPermissionsJson = buildColumnPermissionsJson(dsId, userId);
        String rowPermissionsJson = buildRowPermissionsJson(dsId, userId, userInfo);

        if (columnPermissionsJson.equals("[]") && rowPermissionsJson.equals("[]")) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n### 列权限规则\n");
        sb.append(columnPermissionsJson);
        sb.append("\n\n### 行权限规则\n");
        sb.append(rowPermissionsJson);

        return sb.toString();
    }

    private String buildColumnPermissionsJson(Long dsId, String userId) {
        List<PermissionDTO> columnPermissions = permissionService.getColumnPermissionsByDsId(dsId);
        if (columnPermissions == null || columnPermissions.isEmpty()) {
            return "[]";
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (PermissionDTO permission : columnPermissions) {
            if (!permissionService.isUserInRule(userId, permission.getId())) {
                continue;
            }
            if (permission.getPermissions() != null) {
                for (PermissionDTO.ColumnPermission cp : permission.getPermissions()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("table_name", permission.getTableName() != null ? permission.getTableName() : "*");
                    item.put("field_name", cp.getFieldName());
                    item.put("accessible", cp.getEnable() != null && cp.getEnable());
                    result.add(item);
                }
            }
        }
        return JSONUtil.toJsonStr(result);
    }

    private String buildRowPermissionsJson(Long dsId, String userId, Map<String, Object> userInfo) {
        List<PermissionDTO> rowPermissions = permissionService.getRowPermissionsByDsId(dsId);
        if (rowPermissions == null || rowPermissions.isEmpty()) {
            return "[]";
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (PermissionDTO permission : rowPermissions) {
            if (!permissionService.isUserInRule(userId, permission.getId())) {
                continue;
            }
            if (permission.getExpressionTree() != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("table_name", permission.getTableName() != null ? permission.getTableName() : "*");

                List<Map<String, Object>> conditions = convertExpressionTreeToConditions(
                        permission.getExpressionTree(), userId, userInfo);
                item.put("conditions", conditions);
                result.add(item);
            }
        }
        return JSONUtil.toJsonStr(result);
    }

    private List<Map<String, Object>> convertExpressionTreeToConditions(
            PermissionDTO.ExpressionTree tree, String userId, Map<String, Object> userInfo) {
        List<Map<String, Object>> conditions = new ArrayList<>();
        if (tree == null || tree.getItems() == null) {
            return conditions;
        }

        for (PermissionDTO.ExpressionItem item : tree.getItems()) {
            if ("item".equals(item.getType())) {
                Map<String, Object> condition = new HashMap<>();
                condition.put("field", item.getFieldName());
                condition.put("operator", item.getTerm());

                Object value = item.getValue();
                if (value instanceof String str && str.startsWith("$") && str.endsWith("$")) {
                    String varName = str.substring(1, str.length() - 1);
                    if ("user_id".equals(varName) || "userId".equals(varName)) {
                        value = userId;
                    } else if (userInfo != null && userInfo.containsKey(varName)) {
                        value = userInfo.get(varName);
                    }
                }
                condition.put("value", value);
                condition.put("logic", item.getFilterType() != null ? item.getFilterType().toUpperCase() : "AND");

                conditions.add(condition);
            } else if ("tree".equals(item.getType()) && item.getSubTree() != null) {
                conditions.addAll(convertExpressionTreeToConditions(item.getSubTree(), userId, userInfo));
            }
        }
        return conditions;
    }

    private String buildUserContextJson(String userId, Map<String, Object> userInfo) {
        Map<String, Object> context = new HashMap<>();
        context.put("user_id", userId);

        if (userInfo != null) {
            context.putAll(userInfo);
        }

        return JSONUtil.toJsonStr(context);
    }
}
