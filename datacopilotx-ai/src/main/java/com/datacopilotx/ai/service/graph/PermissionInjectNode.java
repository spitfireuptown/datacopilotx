package com.datacopilotx.ai.service.graph;

import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.service.PermissionService;
import com.datacopilotx.ai.service.graph.main.WorkflowServiceHelper;
import com.datacopilotx.ai.service.graph.main.SerializableSink;
import com.datacopilotx.ai.service.graph.main.WorkflowState;
import com.datacopilotx.ai.util.SecurityUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class PermissionInjectNode implements NodeAction<WorkflowState> {

    @Resource
    private PermissionService permissionService;

    @Resource
    private WorkflowServiceHelper workflowServiceHelper;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        DataSetBean dataSetBean = state.getDataSetBean();
        String sql = state.sql().orElseThrow(() -> new IllegalArgumentException("sql is empty"));

        String currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null || SecurityUtil.isAdmin()) {
            return Map.of("sql", sql);
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("user_id", currentUserId);

        String rowFilter = permissionService.getRowPermissionFilter(dataSetBean.getId(), currentUserId, userInfo);
        
        if (!rowFilter.isEmpty()) {
            String securedSql = injectRowPermission(sql, rowFilter);
            log.info("注入行权限过滤条件，原SQL: {}, 注入后: {}", sql, securedSql);
            
            Sinks.Many<org.springframework.http.codec.ServerSentEvent<com.datacopilotx.common.result.WebResult<String>>> sink = state.getSink();
            SerializableSink serializableSink = state.getSerializableSink();
            workflowServiceHelper.streamPrint(sink, "权限注入", "\n应用数据权限过滤...", serializableSink, state);
            
            return Map.of("sql", securedSql);
        }

        return Map.of("sql", sql);
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
}