package com.datacopilotx.ai.service.graph;

import cn.hutool.json.JSONUtil;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.dto.DataSetDTO;
import com.datacopilotx.ai.domian.dto.QueryDTO;
import com.datacopilotx.ai.service.driver.DriverFactory;
import com.datacopilotx.ai.service.driver.base.JDBCDriver;
import com.datacopilotx.ai.util.ExceptionUtil;
import com.datacopilotx.ai.service.graph.main.WorkflowGraph;
import com.datacopilotx.ai.service.graph.main.WorkflowServiceHelper;
import com.datacopilotx.ai.service.graph.main.SerializableSink;
import com.datacopilotx.ai.service.graph.main.WorkflowState;
import com.datacopilotx.common.constant.PromptConstant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ExecuteSQLGraphNode implements NodeAction<WorkflowState> {

    @Resource
    WorkflowServiceHelper workflowServiceHelper;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        DataSetBean dataSetBean = state.getDataSetBean();
        String sql = state.sql().orElseThrow(() -> new IllegalArgumentException("sql is empty"));

        log.info("Executing SQL: {}", sql);

        DataSetDTO.DriverInfo driverInfo = DataSetDTO.DriverInfo
                .builder()
                .host(dataSetBean.getHost())
                .port(dataSetBean.getPort())
                .database(dataSetBean.getDatabase())
                .username(dataSetBean.getUsername())
                .password(dataSetBean.getPassword())
                .type(dataSetBean.getType())
                .build();

        try {
            JDBCDriver driver = DriverFactory.getDriver(driverInfo);
            QueryDTO queryDTO = driver.execute(driverInfo, sql);

            Sinks.Many<org.springframework.http.codec.ServerSentEvent<com.datacopilotx.common.result.WebResult<String>>> sink = state.getSink();
            SerializableSink serializableSink = state.getSerializableSink();
            workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_RESULT_NODE, "\n", serializableSink, state);
            workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_RESULT_NODE, "#### 问数结果: ", serializableSink, state);
            workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_RESULT_NODE, JSONUtil.toJsonStr(queryDTO), serializableSink, state);

            String resultJson = JSONUtil.toJsonStr(queryDTO);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("result", resultJson);
            resultMap.put("answer", "SQL执行结果:\n" + resultJson);
            resultMap.put("sql_error", "");
            resultMap.putAll(state.appendCollectedData("\n#### 问数结果: " + resultJson));
            return resultMap;
        } catch (Exception e) {
            int retryCount = state.retryCount().orElse(0) + 1;

            Sinks.Many<org.springframework.http.codec.ServerSentEvent<com.datacopilotx.common.result.WebResult<String>>> sink = state.getSink();
            SerializableSink serializableSink = state.getSerializableSink();

            if (retryCount >= WorkflowGraph.MAX_RETRY) {
                log.error("SQL执行最终失败（已重试{}次），SQL: {}, 异常: ", retryCount, sql, e);
                workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_EXECUTION_NODE,
                        "\nSQL执行最终失败（已重试" + retryCount + "次），错误信息: " + e.getMessage() + "\n", serializableSink, state);
            } else {
                log.error("SQL执行失败（第{}次重试）: {}", retryCount, e.getMessage(), e);
                workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_EXECUTION_NODE,
                        "\nSQL执行失败，正在重试" + retryCount + "/" + WorkflowGraph.MAX_RETRY + "...\n", serializableSink, state);
            }

            String sqlError = "上一次生成的SQL: " + sql + "\n执行报错: " + ExceptionUtil.getFullStackTrace(e) + "\n请根据错误信息修复SQL并重新生成。";
            return Map.of(
                "sql_error", sqlError,
                "retry_count", retryCount
            );
        }
    }
}
