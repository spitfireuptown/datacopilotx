package com.datacopilotx.ai.service.flow;

import com.datacopilotx.ai.controller.form.QuestionForm;
import org.springframework.http.codec.ServerSentEvent;
import com.datacopilotx.common.result.WebResult;
import reactor.core.publisher.Sinks;

import java.util.Map;

/**
 * 工作流节点接口
 */
public interface ChatProcessStep {
    /**
     * 执行当前步骤的处理逻辑
     * @param sink SSE发送器
     * @param questionForm 问题表单
     */
    void process(Sinks.Many<ServerSentEvent<WebResult<String>>> sink, Map<String, Object> preResultMap, QuestionForm questionForm);
    
    /**
     * 设置下一个处理步骤
     * @param nextStep 下一个步骤
     * @return 当前步骤，用于链式调用
     */
    ChatProcessStep setNextStep(ChatProcessStep nextStep);
    
    /**
     * 获取下一个处理步骤
     * @return 下一个步骤
     */
    ChatProcessStep getNextStep();
}