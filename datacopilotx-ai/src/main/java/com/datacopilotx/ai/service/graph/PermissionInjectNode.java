package com.datacopilotx.ai.service.graph;

import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.DataTableBean;
import com.datacopilotx.ai.mapper.DataTableMapper;
import com.datacopilotx.ai.service.PermissionService;
import com.datacopilotx.ai.service.graph.main.WorkflowServiceHelper;
import com.datacopilotx.ai.service.graph.main.WorkflowState;
import com.datacopilotx.ai.util.ColumnPermissionUtil;
import com.datacopilotx.ai.util.SecurityUtil;
import com.datacopilotx.common.constant.PromptConstant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class PermissionInjectNode implements NodeAction<WorkflowState> {

    @Resource
    private PermissionService permissionService;

    @Resource
    private WorkflowServiceHelper workflowServiceHelper;

    @Resource
    private DataTableMapper dataTableMapper;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        DataSetBean dataSetBean = state.getDataSetBean();
        String sql = state.sql().orElseThrow(() -> new IllegalArgumentException("sql is empty"));

        String currentUserId = state.userId().orElse(null);
        Integer currentUserRole = state.userRole().orElse(2);

        var sink = state.getSink();
        var serializableSink = state.getSerializableSink();

        workflowServiceHelper.streamPrint(sink, PromptConstant.PERMISSION_INJECT_NODE, "\n", serializableSink, state);
        workflowServiceHelper.streamPrint(sink, PromptConstant.PERMISSION_INJECT_NODE, "\n", serializableSink, state);
        workflowServiceHelper.streamPrint(sink, PromptConstant.PERMISSION_INJECT_NODE, "#### 权限注入: ", serializableSink, state);
        workflowServiceHelper.streamPrint(sink, PromptConstant.PERMISSION_INJECT_NODE, "\n", serializableSink, state);

        if (SecurityUtil.isAdmin(currentUserRole)) {
            log.info("用户未登录或为管理员，跳过权限注入");
            workflowServiceHelper.streamPrint(sink, PromptConstant.PERMISSION_INJECT_NODE, "用户未登录或为管理员，跳过权限注入\n", serializableSink, state);
            return Map.of("sql", sql);
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("user_id", currentUserId);

        workflowServiceHelper.streamPrint(sink, PromptConstant.PERMISSION_INJECT_NODE, "\n应用数据权限过滤...\n", serializableSink, state);

        Long tableId = extractTableIdFromSql(sql, dataSetBean.getId());

        String rowFilter = permissionService.getRowPermissionFilter(dataSetBean.getId(), tableId, currentUserId, userInfo);
        String securedSql = sql;

        if (!rowFilter.isEmpty()) {
            securedSql = injectRowPermission(securedSql, rowFilter);
            log.info("注入行权限过滤条件，原SQL: {}, 注入后: {}", sql, securedSql);
            workflowServiceHelper.streamPrint(sink, PromptConstant.PERMISSION_INJECT_NODE, "行权限过滤条件: " + rowFilter + "\n", serializableSink, state);
        } else {
            workflowServiceHelper.streamPrint(sink, PromptConstant.PERMISSION_INJECT_NODE, "未配置行权限\n", serializableSink, state);
        }

        List<String> allFields = ColumnPermissionUtil.extractFieldsFromSql(sql);
        List<String> allowedFields = permissionService.getColumnPermissionFields(dataSetBean.getId(), tableId, currentUserId, allFields);

        if (allowedFields != null && allowedFields.size() < allFields.size()) {
            String columnFilteredSql = ColumnPermissionUtil.rewriteAggregateSql(securedSql, allowedFields);
            if (columnFilteredSql != null) {
                securedSql = columnFilteredSql;
                List<String> hiddenFields = allFields.stream().filter(f -> !allowedFields.contains(f)).toList();
                log.info("注入列权限过滤，隐藏字段: {}, 改写后SQL: {}", hiddenFields, securedSql);
                workflowServiceHelper.streamPrint(sink, PromptConstant.PERMISSION_INJECT_NODE, "列权限过滤，隐藏字段: " + hiddenFields + "\n", serializableSink, state);
            } else {
                securedSql = "SELECT 1 FROM " + extractFromPart(sql) + " WHERE 1=0";
                workflowServiceHelper.streamPrint(sink, PromptConstant.PERMISSION_INJECT_NODE, "无权限访问任何字段\n", serializableSink, state);
            }
        } else {
            workflowServiceHelper.streamPrint(sink, PromptConstant.PERMISSION_INJECT_NODE, "未配置列权限或拥有全部字段权限\n", serializableSink, state);
        }

        return Map.of("sql", securedSql);
    }

    private String injectRowPermission(String sql, String filter) {
        String lowerSql = sql.toLowerCase().trim();

        if (lowerSql.startsWith("select")) {
            int whereIndex = lowerSql.indexOf(" where ");
            if (whereIndex > 0) {
                return sql.substring(0, whereIndex) + " WHERE " + filter + " AND " + sql.substring(whereIndex + 7);
            } else {
                int groupByIndex = lowerSql.indexOf(" group by ");
                int orderByIndex = lowerSql.indexOf(" order by ");
                int limitIndex = lowerSql.indexOf(" limit ");

                int insertIndex = sql.length();
                if (groupByIndex > 0) insertIndex = Math.min(insertIndex, groupByIndex);
                if (orderByIndex > 0) insertIndex = Math.min(insertIndex, orderByIndex);
                if (limitIndex > 0) insertIndex = Math.min(insertIndex, limitIndex);

                return sql.substring(0, insertIndex) + " WHERE " + filter + " " + sql.substring(insertIndex);
            }
        }

        return sql;
    }

    private String extractFromPart(String sql) {
        String lowerSql = sql.toLowerCase();
        int fromIndex = lowerSql.indexOf(" from ");
        if (fromIndex > 0) {
            int endIndex = sql.length();
            int whereIndex = lowerSql.indexOf(" where ");
            int groupByIndex = lowerSql.indexOf(" group by ");
            int orderByIndex = lowerSql.indexOf(" order by ");
            int limitIndex = lowerSql.indexOf(" limit ");

            if (whereIndex > 0) endIndex = Math.min(endIndex, whereIndex);
            if (groupByIndex > 0) endIndex = Math.min(endIndex, groupByIndex);
            if (orderByIndex > 0) endIndex = Math.min(endIndex, orderByIndex);
            if (limitIndex > 0) endIndex = Math.min(endIndex, limitIndex);

            return sql.substring(fromIndex + 6, endIndex).trim();
        }
        return "";
    }

    private Long extractTableIdFromSql(String sql, Long dsId) {
        String fromPart = extractFromPart(sql);
        if (fromPart.isEmpty()) {
            return null;
        }

        String tableName = extractTableName(fromPart);
        if (tableName == null || tableName.isEmpty()) {
            return null;
        }

        try {
            List<DataTableBean> tables = dataTableMapper.selectList(new LambdaQueryWrapper<DataTableBean>()
                    .eq(DataTableBean::getDatasetId, dsId)
                    .eq(DataTableBean::getIsDel, 0));

            Optional<DataTableBean> matchedTable = tables.stream()
                    .filter(t -> tableName.equalsIgnoreCase(t.getTable()))
                    .findFirst();

            return matchedTable.map(DataTableBean::getId).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to extract tableId from SQL: {}", e.getMessage());
            return null;
        }
    }

    private String extractTableName(String fromPart) {
        String trimmed = fromPart.trim();
        if (trimmed.startsWith("`")) {
            int endIndex = trimmed.indexOf("`", 1);
            if (endIndex > 0) {
                return trimmed.substring(1, endIndex);
            }
        }

        String[] parts = trimmed.split("[\\s,.`]+");
        for (String part : parts) {
            if (!part.isEmpty() && !part.equalsIgnoreCase("as")) {
                return part;
            }
        }
        return null;
    }
}