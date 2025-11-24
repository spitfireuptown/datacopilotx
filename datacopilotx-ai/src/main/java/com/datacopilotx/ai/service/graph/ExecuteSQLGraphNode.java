package com.datacopilotx.ai.service.graph;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.datacopilotx.ai.controller.form.QuestionForm;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.QuestionLogBean;
import com.datacopilotx.ai.domian.dto.DataSetDTO;
import com.datacopilotx.ai.domian.dto.QueryDTO;
import com.datacopilotx.ai.mapper.QuestionLogMapper;
import com.datacopilotx.ai.service.driver.DriverFactory;
import com.datacopilotx.ai.service.driver.base.JDBCDriver;
import com.datacopilotx.ai.service.flow.AbstractChatProcessStep;
import com.datacopilotx.ai.service.flow.WorkflowServiceHelper;
import com.datacopilotx.common.constant.PromptConstant;
import com.datacopilotx.common.exception.DataCopilotXException;
import com.datacopilotx.common.result.WebResult;
import lombok.Data;
import lombok.SneakyThrows;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.Map;

@Component
public class ExecuteSQLGraphNode implements NodeAction<WorkflowState> {

    @Autowired
    QuestionLogMapper questionLogMapper;
    @Autowired
    WorkflowServiceHelper workflowServiceHelper;

    @Override
    public Map<String, Object> apply(WorkflowState workflowState) throws Exception {
        Sinks.Many<ServerSentEvent<WebResult<String>>> sink = workflowState.sink().orElseThrow(() -> new DataCopilotXException("sink is empty"));
        DataSetBean dataSetBean = workflowState.dataSetBean().orElseThrow(() -> new DataCopilotXException("dataSetBean is empty"));

        String questionId = workflowState.questionId().orElseThrow(() -> new DataCopilotXException("questionId is empty"));
        String sessionId = workflowState.sessionId().orElseThrow(() -> new DataCopilotXException("sessionId is empty"));
        String sql = workflowState.sql().orElseThrow(() -> new DataCopilotXException("sql is empty"));

        workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_RESULT_NODE, "\n");
        workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_RESULT_NODE, "#### 问数结果: ");

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
        sink.tryEmitNext(workflowServiceHelper.buildSseEvent(PromptConstant.SQL_RESULT_NODE, WebResult.success(JSONUtil.toJsonStr(queryDTO))));
        questionLogMapper.update(
                questionLogBean,
                new LambdaUpdateWrapper<QuestionLogBean>()
                        .eq(QuestionLogBean::getQuestionId, questionId)
                        .eq(QuestionLogBean::getSessionId, sessionId)
        );
        sink.tryEmitComplete();
        return Map.of();
    }
}
