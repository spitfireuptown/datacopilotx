package com.datacopilotx.ai.service.graph;

import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONUtil;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.ModelConfigBean;
import com.datacopilotx.ai.service.flow.WorkflowServiceHelper;
import com.datacopilotx.ai.service.graph.main.WorkflowState;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.common.constant.PromptConstant;
import com.datacopilotx.common.result.WebResult;
import com.datacopilotx.common.util.WorkflowUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Slf4j
@Component
public class GenerateSqlGraphNode implements NodeAction<WorkflowState> {
    @Resource
    private AIGatewayChatService aiGatewayChatService;

    @Resource
    private WorkflowServiceHelper workflowServiceHelper;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        String beautifulQuestion = state.beautifulQuestion().orElseThrow(() -> new IllegalArgumentException("beautifulQuestion is null"));
        ModelConfigBean modelConfigBean = state.getModelConfigBean();
        DataSetBean dataSetBean = state.getDataSetBean();

        List<String> recall = state.recall();

        Pair<String, String> promptPair = workflowServiceHelper.injectPrompt(
                PromptConstant.SQL_GENERATION_PROMPT,
                Map.of(
                        "${time}", workflowServiceHelper.getCurrentTime(),
                        "${query}", beautifulQuestion,
                        "${engine}", modelConfigBean.getType(),
                        "${innerPrompt}", ObjectUtils.isEmpty(dataSetBean.getInjectPrompt()) ? "" : dataSetBean.getInjectPrompt(),
                        "${recall}", ObjectUtils.isEmpty(recall) ? "" : JSONUtil.toJsonStr(recall),
                        "${meta}", workflowServiceHelper.assembleDataSetInfo(dataSetBean)
                )
        );

        ChatRequest chatRequest = state.buildLLMRequest();
        chatRequest.setSystemPrompt(promptPair.getKey());
        chatRequest.setUserPrompt(promptPair.getValue());

        StringBuilder resultBuilder = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        var sink = state.getSink();

        aiGatewayChatService.streamChatCompletions(chatRequest)
                .doOnNext(chunk -> {
                    resultBuilder.append(chunk);
                    if (sink != null) {
                        sink.tryEmitNext(ServerSentEvent.<WebResult<String>>builder()
                                .event("node_progress")
                                .data(WebResult.success(chunk))
                                .build());
                    }
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

        String generateSqlResult = WorkflowUtil.cleanJsonStr(resultBuilder.toString());
        Map<String, Object> generateSqlResultMap = JSONUtil.toBean(generateSqlResult, Map.class);
        String sql = (String) generateSqlResultMap.get("sql");

        if (sink != null) {
            sink.tryEmitNext(ServerSentEvent.<WebResult<String>>builder()
                    .event("node_progress")
                    .data(WebResult.success("SQL生成完成"))
                    .build());
        }

        log.info("Generated SQL: {}", sql);
        return Map.of(
                "sql", sql,
                "token", chatRequest.getTokenUsage()
        );
    }
}
