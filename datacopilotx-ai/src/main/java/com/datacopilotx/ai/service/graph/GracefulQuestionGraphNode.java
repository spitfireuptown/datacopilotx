package com.datacopilotx.ai.service.graph;

import cn.hutool.core.lang.Pair;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datacopilotx.ai.domian.bean.QuestionLogBean;
import com.datacopilotx.ai.mapper.QuestionLogMapper;
import com.datacopilotx.ai.service.graph.main.WorkflowServiceHelper;
import com.datacopilotx.ai.service.graph.main.WorkflowState;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.common.constant.PromptConstant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Slf4j
@Component
public class GracefulQuestionGraphNode implements NodeAction<WorkflowState> {

    private static final int MAX_HISTORY_ROUNDS = 5;

    @Resource
    private AIGatewayChatService aiGatewayChatService;

    @Resource
    private WorkflowServiceHelper workflowServiceHelper;

    @Resource
    private QuestionLogMapper questionLogMapper;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        String questionId = state.questionId().orElseThrow(() -> new IllegalArgumentException("questionId is empty"));
        String question = state.question().orElseThrow(() -> new IllegalArgumentException("question is empty"));
        String sessionId = state.sessionId().orElseThrow(() -> new IllegalArgumentException("sessionId is empty"));

        String history = buildHistoryContext(sessionId, questionId);

        Pair<String, String> promptPair = workflowServiceHelper.injectPrompt(
                PromptConstant.BEAUTIFUL_PROMPT,
                Map.of("${query}", question, "${history}", history)
        );

        log.info("question_id: {}, beautifulQuestion: {}", questionId, promptPair);
        ChatRequest chatRequest = state.buildLLMRequest(question);
        chatRequest.setSystemPrompt(promptPair.getKey());
        chatRequest.setUserPrompt(promptPair.getValue());

        StringBuilder resultBuilder = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        var sink = state.getSink();
        var serializableSink = state.getSerializableSink();

        workflowServiceHelper.streamPrint(sink, PromptConstant.BEAUTIFUL_NODE, "#### 用户问题: ", serializableSink, state);
        workflowServiceHelper.streamPrint(sink, PromptConstant.BEAUTIFUL_NODE, "\n", serializableSink, state);

        aiGatewayChatService.streamChatCompletions(chatRequest)
                .doOnNext(chunk -> {
                    resultBuilder.append(chunk);
                    workflowServiceHelper.streamPrint(sink, PromptConstant.BEAUTIFUL_NODE, chunk, serializableSink, state);
                })
                .doOnComplete(latch::countDown)
                .doOnError(e -> {
                    log.error("流式输出异常: {}", e.getMessage());
                    latch.countDown();
                })
                .subscribe();

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("等待流式输出完成被中断", e);
        }

        String result = resultBuilder.toString();
        
        state.appendCollectedData(result);

        return Map.of(
                "beautiful_question", result,
                "answer", result,
                "token", chatRequest.getTokenUsage()
        );
    }

    private String buildHistoryContext(String sessionId, String currentQuestionId) {
        List<QuestionLogBean> historyLogs = questionLogMapper.selectList(
                new LambdaQueryWrapper<QuestionLogBean>()
                        .eq(QuestionLogBean::getSessionId, sessionId)
                        .ne(QuestionLogBean::getQuestionId, currentQuestionId)
                        .eq(QuestionLogBean::getIsDel, 0)
                        .orderByDesc(QuestionLogBean::getCtime)
                        .last("LIMIT " + MAX_HISTORY_ROUNDS)
        );

        if (historyLogs == null || historyLogs.isEmpty()) {
            return "无历史对话";
        }

        StringBuilder historyBuilder = new StringBuilder();
        for (int i = historyLogs.size() - 1; i >= 0; i--) {
            QuestionLogBean log = historyLogs.get(i);
            historyBuilder.append(String.format("用户: %s\n", log.getQuestion()));
            historyBuilder.append(String.format("助手: %s\n\n", log.getAnswer() != null ? log.getAnswer() : ""));
        }
        return historyBuilder.toString().trim();
    }
}