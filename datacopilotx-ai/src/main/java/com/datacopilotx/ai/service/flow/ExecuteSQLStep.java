package com.datacopilotx.ai.service.flow;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.datacopilotx.ai.controller.form.QuestionForm;
import com.datacopilotx.ai.domian.bean.QuestionLogBean;
import com.datacopilotx.ai.domian.dto.DataSetDTO;
import com.datacopilotx.ai.domian.dto.QueryDTO;
import com.datacopilotx.ai.mapper.QuestionLogMapper;
import com.datacopilotx.ai.service.driver.DriverFactory;
import com.datacopilotx.ai.service.driver.base.JDBCDriver;
import com.datacopilotx.common.constant.PromptConstant;
import com.datacopilotx.common.result.WebResult;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.Map;

@Component
public class ExecuteSQLStep extends AbstractChatProcessStep {

    @Autowired
    QuestionLogMapper questionLogMapper;
    @Autowired
    WorkflowServiceHelper workflowServiceHelper;


    @SneakyThrows
    @Override
    public void process(Sinks.Many<ServerSentEvent<WebResult<String>>> sink, Map<String, Object> preResultMap, QuestionForm questionForm) {
        workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_RESULT_NODE, "\n", questionForm);
        workflowServiceHelper.streamPrint(sink, PromptConstant.SQL_RESULT_NODE, "#### 问数结果: ", questionForm);

        DataSetDTO.DriverInfo driverInfo = DataSetDTO.DriverInfo
                .builder()
                .host(questionForm.getDataSetBean().getHost())
                .port(questionForm.getDataSetBean().getPort())
                .database(questionForm.getDataSetBean().getDatabase())
                .table(questionForm.getDataSetBean().getTable())
                .username(questionForm.getDataSetBean().getUsername())
                .password(questionForm.getDataSetBean().getPassword())
                .type(questionForm.getDataSetBean().getType())
                .build();

        JDBCDriver driver = DriverFactory.getDriver(driverInfo);
        QueryDTO queryDTO = driver.execute(driverInfo, questionForm.getSql());
        QuestionLogBean questionLogBean = questionForm.getQuestionLogBean();
        questionLogBean.setResult(JSONUtil.toJsonStr(queryDTO));
        sink.tryEmitNext(workflowServiceHelper.buildSseEvent(PromptConstant.SQL_RESULT_NODE, WebResult.success(JSONUtil.toJsonStr(queryDTO))));
        questionLogMapper.update(
                questionLogBean,
                new LambdaUpdateWrapper<QuestionLogBean>()
                        .eq(QuestionLogBean::getQuestionId, questionForm.getQuestionId())
                        .eq(QuestionLogBean::getSessionId, questionForm.getSessionId())
        );
        sink.tryEmitComplete();
    }
}
