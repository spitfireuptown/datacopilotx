package com.datacopilotx.ai.service.graph;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.QuestionLogBean;
import com.datacopilotx.ai.domian.dto.DataSetDTO;
import com.datacopilotx.ai.domian.dto.QueryDTO;
import com.datacopilotx.ai.mapper.QuestionLogMapper;
import com.datacopilotx.ai.service.driver.DriverFactory;
import com.datacopilotx.ai.service.driver.base.JDBCDriver;
import com.datacopilotx.ai.service.graph.main.WorkflowState;
import com.datacopilotx.common.result.WebResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ExecuteSQLGraphNode implements NodeAction<WorkflowState> {

    @Autowired
    QuestionLogMapper questionLogMapper;

    @SneakyThrows
    @Override
    public Map<String, Object> apply(WorkflowState state) {
        DataSetBean dataSetBean = state.getDataSetBean();
        String questionId = state.questionId().orElseThrow(() -> new IllegalArgumentException("questionId is empty"));
        String sessionId = state.sessionId().orElseThrow(() -> new IllegalArgumentException("sessionId is empty"));
        String sql = state.sql().orElseThrow(() -> new IllegalArgumentException("sql is empty"));

        log.info("Executing SQL: {}", sql);

        DataSetDTO.DriverInfo driverInfo = DataSetDTO.DriverInfo
                .builder()
                .host(dataSetBean.getHost())
                .port(dataSetBean.getPort())
                .database(dataSetBean.getDatabase())
                .table(dataSetBean.getTable())
                .username(dataSetBean.getUsername())
                .password(dataSetBean.getPassword())
                .type(dataSetBean.getType())
                .build();

        JDBCDriver driver = DriverFactory.getDriver(driverInfo);
        QueryDTO queryDTO = driver.execute(driverInfo, sql);

        QuestionLogBean questionLogBean = questionLogMapper.selectOne(new LambdaQueryWrapper<QuestionLogBean>()
                .eq(QuestionLogBean::getQuestionId, questionId)
                .eq(QuestionLogBean::getSessionId, sessionId)
        );

        questionLogBean.setResult(JSONUtil.toJsonStr(queryDTO));
        questionLogMapper.updateById(questionLogBean);

        var sink = state.getSink();
        if (sink != null) {
            sink.tryEmitNext(ServerSentEvent.<WebResult<String>>builder()
                    .event("node_progress")
                    .data(WebResult.success("SQL执行完成"))
                    .build());
        }

        String resultJson = JSONUtil.toJsonStr(queryDTO);
        return Map.of(
            "result", resultJson,
            "answer", "SQL执行结果:\n" + resultJson
        );
    }
}
