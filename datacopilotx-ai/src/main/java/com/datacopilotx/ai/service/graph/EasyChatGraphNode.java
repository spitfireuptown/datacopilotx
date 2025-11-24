package com.datacopilotx.ai.service.graph;

import cn.hutool.core.lang.Pair;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.datacopilotx.ai.domian.bean.QuestionLogBean;
import com.datacopilotx.ai.mapper.QuestionLogMapper;
import com.datacopilotx.ai.service.flow.WorkflowServiceHelper;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.common.constant.PromptConstant;
import com.datacopilotx.common.exception.DataCopilotXException;
import com.datacopilotx.common.result.WebResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;

/**
 * 闲聊步骤
 */
@Slf4j
@Component
public class EasyChatGraphNode implements NodeAction<WorkflowState> {
    @Resource
    private AIGatewayChatService aiGatewayChatService;
    @Resource
    private WorkflowServiceHelper workflowServiceHelper;
    @Resource
    private QuestionLogMapper questionLogMapper;

    @Override
    public Map<String, Object> apply(WorkflowState workflowState) throws Exception {
        String beautifulQuestion = workflowState.beautifulQuestion().orElse("");
        Sinks.Many<ServerSentEvent<WebResult<String>>> sink = workflowState.sink().orElseThrow(() -> new DataCopilotXException("sink is empty"));
        
        // 获取questionId和sessionId用于更新日志
        String questionId = workflowState.questionId().orElse("");
        String sessionId = workflowState.sessionId().orElse("");

        Pair<String, String> promptPair = workflowServiceHelper.injectPrompt(
                PromptConstant.EASY_CHAT_PROMPT,
                Map.of(
                "${query}", beautifulQuestion
                )
        );

        workflowServiceHelper.streamPrint(sink, PromptConstant.EASY_CHAT_NODE, "\n");
        workflowServiceHelper.streamPrint(sink, PromptConstant.EASY_CHAT_NODE, "\n");
        workflowServiceHelper.streamPrint(sink, PromptConstant.EASY_CHAT_NODE, "#### 回答: ");
        workflowServiceHelper.streamPrint(sink, PromptConstant.EASY_CHAT_NODE, "\n");

        ChatRequest chatRequest = workflowState.buildLLMRequest();
        chatRequest.setSystemPrompt(promptPair.getKey());
        chatRequest.setUserPrompt(promptPair.getValue());
        
        // 使用StringBuilder来累积回答内容
        StringBuilder answerBuilder = new StringBuilder();

        aiGatewayChatService.streamChatCompletions(chatRequest)
                .flatMap(response -> {
                    // 追加回答内容
                    answerBuilder.append(response);
                    return Mono.just(response);
                })
                .subscribe(
                        (data) -> workflowServiceHelper.streamPrint(sink, PromptConstant.EASY_CHAT_NODE, data),
                        (error) -> workflowServiceHelper.errorHandling(PromptConstant.EASY_CHAT_NODE, sink, "闲聊异常" + error.getMessage()),
                        () -> {
                            // 使用累积的回答内容
                            String answer = answerBuilder.toString();
                            QuestionLogBean questionLogBean = questionLogMapper.selectOne(new LambdaQueryWrapper<QuestionLogBean>()
                                            .eq(QuestionLogBean::getQuestionId, questionId)
                                            .eq(QuestionLogBean::getSessionId, sessionId)
                            );
                            questionLogBean.setAnswer(questionLogBean.getAnswer() + answer);
                            questionLogMapper.updateById(questionLogBean);
                            sink.tryEmitComplete();
                        }
                );
                
        // 返回回答内容供后续使用
        return Map.of();
    }
}