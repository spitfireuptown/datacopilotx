package com.datacopilotx.ai.service.flow;


import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.datacopilotx.ai.controller.form.QuestionForm;
import com.datacopilotx.ai.domian.bean.QuestionLogBean;
import com.datacopilotx.ai.mapper.QuestionLogMapper;
import com.datacopilotx.common.result.WebResult;
import org.slf4j.MDC;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;

import java.util.Map;

/**
 * 抽象工作流节点基础类
 */
public abstract class AbstractChatProcessStep implements ChatProcessStep, ApplicationContextAware {

    private ApplicationContext applicationContext;
    private ChatProcessStep nextStep;
    
    @Override
    public ChatProcessStep setNextStep(ChatProcessStep nextStep) {
        this.nextStep = nextStep;
        return nextStep;
    }
    
    @Override
    public ChatProcessStep getNextStep() {
        return nextStep;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    /**
     * 执行下一个步骤
     * @param sink SSE发送器
     * @param preResultMap 上一步的输出信息
     * @param questionForm 问题表单
     */
    public void proceedToNextStep(Sinks.Many<ServerSentEvent<WebResult<String>>> sink, Map<String, Object> preResultMap, QuestionForm questionForm) {
        // 调用下一步前保存上一步的输出信息
        QuestionLogMapper questionLogMapper = this.applicationContext.getBean(QuestionLogMapper.class);
        MDC.put("trace_id", questionForm.getQuestionId());

        questionLogMapper.update(
                new LambdaUpdateWrapper<QuestionLogBean>()
                        .set(QuestionLogBean::getAnswer, questionForm.getAnswer())
                        .set(QuestionLogBean::getCostToken, questionForm.getToken())
                        .set(QuestionLogBean::getCostTime, questionForm.getTimeCost())
                        .eq(QuestionLogBean::getQuestionId, questionForm.getQuestionId())
                        .eq(QuestionLogBean::getSessionId, questionForm.getSessionId())
        );

        if (getNextStep() != null) {
            getNextStep().process(sink, preResultMap, questionForm);
        }
    }
}