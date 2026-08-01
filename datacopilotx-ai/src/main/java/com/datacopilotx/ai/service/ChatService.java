package com.datacopilotx.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datacopilotx.ai.controller.form.QuestionForm;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.ModelConfigBean;
import com.datacopilotx.ai.domian.bean.QuestionLogBean;
import com.datacopilotx.ai.mapper.DataSetMapper;
import com.datacopilotx.ai.mapper.ModelConfigMapper;
import com.datacopilotx.ai.mapper.QuestionLogMapper;
import com.datacopilotx.ai.service.graph.main.SerializableSink;
import com.datacopilotx.ai.service.graph.main.WorkflowGraph;
import com.datacopilotx.ai.service.graph.main.WorkflowServiceHelper;
import com.datacopilotx.ai.service.graph.main.WorkflowState;
import com.datacopilotx.ai.util.SecurityUtil;
import com.datacopilotx.harness.agent.context.AgentContext;
import com.datacopilotx.harness.agent.domain.AttributionReport;
import com.datacopilotx.harness.agent.orchestrator.AgentOrchestrator;
import org.springframework.util.ObjectUtils;
import com.datacopilotx.common.constant.PromptConstant;
import com.datacopilotx.common.result.WebResult;
import com.datacopilotx.common.util.IdUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ChatService {
    @Resource
    private QuestionLogMapper questionLogMapper;
    @Resource
    private DataSetMapper dataSourceMapper;
    @Resource
    private ModelConfigMapper modelConfigMapper;
    @Resource
    private WorkflowGraph workflowGraph;
    @Resource
    private WorkflowServiceHelper flowServiceHelper;
    @Resource
    private AgentOrchestrator agentOrchestrator;


    // 问数入口
    public Flux<ServerSentEvent<WebResult<String>>> chatCompletions(QuestionForm questionForm) {
        Sinks.Many<ServerSentEvent<WebResult<String>>> sink = Sinks.many().unicast().onBackpressureBuffer();

        // 校验数据集
        DataSetBean dataSetBean = dataSourceMapper.selectOne(
                new LambdaQueryWrapper<DataSetBean>()
                        .eq(DataSetBean::getId, questionForm.getDatasetId())
        );

        if (ObjectUtils.isEmpty(dataSetBean)) {
            flowServiceHelper.errorHandling(PromptConstant.START_NODE, sink, "指定数据集不存在");
            return sink.asFlux();
        }
        questionForm.setDataSetBean(dataSetBean);

        ModelConfigBean modelConfigBean = modelConfigMapper.selectOne(new LambdaQueryWrapper<ModelConfigBean>().eq(ModelConfigBean::getId, questionForm.getModelId()));
        if (ObjectUtils.isEmpty(modelConfigBean)) {
            flowServiceHelper.errorHandling(PromptConstant.START_NODE, sink, "指定模型不存在");
            return sink.asFlux();
        }
        questionForm.setModelConfigBean(modelConfigBean);

        String questionId = ObjectUtils.isEmpty(questionForm.getQuestionId()) ? IdUtils.genKey("ques") : questionForm.getQuestionId();
        String sessionId = ObjectUtils.isEmpty(questionForm.getSessionId()) ? IdUtils.genKey("sess") : questionForm.getSessionId();

        // 获取当前登录用户信息（在主线程获取，因为虚拟线程无法继承 SecurityContext）
        String creator = SecurityUtil.getCurrentUserId();
        Integer userRole = SecurityUtil.getCurrentUserRole();
        boolean isAdmin = SecurityUtil.isAdmin();

        QuestionLogBean questionLogBean = QuestionLogBean
                .builder()
                .questionId(questionId)
                .sessionId(sessionId)
                .datasetId(questionForm.getDatasetId())
                .modelId(questionForm.getModelId())
                .question(questionForm.getQuestion())
                .creator(creator)
                .build();
        questionLogMapper.insert(questionLogBean);
        questionForm.setQuestionLogBean(questionLogBean);

        Thread.startVirtualThread(() -> {
            try {
                log.info("Building workflow state graph...");
                StateGraph<WorkflowState> workflow = workflowGraph.createResearchGraph();

                log.info("Compiling workflow graph...");
                CompiledGraph<WorkflowState> compiledGraph = workflow.compile();

                Map<String, Object> initialData = workflowGraph.createInitialState(
                        sessionId,
                        questionId,
                        questionForm.getDatasetId(),
                        questionForm.getModelId(),
                        questionForm.getQuestion(),
                        creator,
                        userRole,
                        isAdmin
                );

                initialData = new HashMap<>(initialData);
                initialData.put("sink", new SerializableSink(sink));
                initialData.put("data_set_bean", dataSetBean);
                initialData.put("model_config_bean", modelConfigBean);

                RunnableConfig runnableConfig = RunnableConfig.builder()
                        .threadId(sessionId)
                        .build();

                log.info("Starting workflow execution, initial state keys: {}", initialData.keySet());

                WorkflowState finalState = null;
                int nodeCount = 0;

                for (var nodeOutput : compiledGraph.stream(initialData, runnableConfig)) {
                    nodeCount++;
                    finalState = nodeOutput.state();
                    log.debug("Executed node {}, current state: {}", nodeCount, finalState);
                }

                if (finalState == null) {
                    throw new IllegalStateException("Workflow execution did not return any state");
                }
                
                // 将收集到的数据保存到question_log（通过 collected_data channel，各节点已包含完整头部标记）
                String collectedData = finalState.getCollectedData();
                
                QuestionLogBean updateLogBean = QuestionLogBean.builder()
                        .questionId(questionId)
                        .sessionId(sessionId)
                        .answer(collectedData)
                        .sql(finalState.sql().orElse(null))
                        .result(finalState.result().orElse(null))
                        .costToken(finalState.token().map(Long::valueOf).orElse(null))
                        .costTime(finalState.timeCost().map(String::valueOf).orElse(null))
                        .build();
                questionLogMapper.update(updateLogBean, 
                        new LambdaQueryWrapper<QuestionLogBean>()
                                .eq(QuestionLogBean::getQuestionId, questionId)
                                .eq(QuestionLogBean::getSessionId, sessionId));
                log.info("Saved sink data, sql and result to question_log for questionId: {}", questionId);
                // 清理ThreadLocal
                finalState.clearCollectedData();
                sink.tryEmitNext(ServerSentEvent.<WebResult<String>>builder()
                        .event("complete")
                        .data(WebResult.success("[DONE]"))
                        .build());
                
                sink.tryEmitComplete();
            } catch (Exception e) {
                log.error("Chat completions execution failed", e);
                sink.tryEmitNext(ServerSentEvent.<WebResult<String>>builder()
                        .event("error")
                        .data(WebResult.error(500, "执行异常: " + e.getMessage()))
                        .build());
                sink.tryEmitComplete();
            }
        });

        return sink.asFlux();
    }

    /**
     * 归因分析入口 —— 对智能问数结果进行归因分析
     * <p>
     * 流程：
     * <ol>
     *   <li>根据 QuestionForm 构建 AgentContext</li>
     *   <li>调用分布式编排器执行 Planner → Executor → Synthesizer 全流程</li>
     *   <li>以 SSE 流式返回归因分析报告</li>
     * </ol>
     */
    public Flux<ServerSentEvent<WebResult<String>>> attributionAnalysis(QuestionForm questionForm) {
        Sinks.Many<ServerSentEvent<WebResult<String>>> sink = Sinks.many().unicast().onBackpressureBuffer();

        String sessionId = ObjectUtils.isEmpty(questionForm.getSessionId())
                ? IdUtils.genKey("attr") : questionForm.getSessionId();

        // 校验数据集
        DataSetBean dataSetBean = dataSourceMapper.selectOne(
                new LambdaQueryWrapper<DataSetBean>()
                        .eq(DataSetBean::getId, questionForm.getDatasetId())
        );
        if (ObjectUtils.isEmpty(dataSetBean)) {
            flowServiceHelper.errorHandling(PromptConstant.START_NODE, sink, "指定数据集不存在");
            return sink.asFlux();
        }

        // 查询模型配置（归因分析需要调用 LLM）
        ModelConfigBean modelConfigBean = modelConfigMapper.selectOne(
                new LambdaQueryWrapper<ModelConfigBean>()
                        .eq(ModelConfigBean::getId, questionForm.getModelId())
        );
        if (ObjectUtils.isEmpty(modelConfigBean)) {
            flowServiceHelper.errorHandling(PromptConstant.START_NODE, sink, "指定模型不存在");
            return sink.asFlux();
        }

        QuestionLogBean questionLogBean = QuestionLogBean.builder()
                .questionId(questionForm.getQuestionId())
                .sessionId(sessionId)
                .datasetId(questionForm.getDatasetId())
                .modelId(questionForm.getModelId())
                .question(questionForm.getQuestion())
                .creator(SecurityUtil.getCurrentUserId())
                .build();
        questionLogMapper.insert(questionLogBean);

        Thread.startVirtualThread(() -> {
            try {
                // 构建 AgentContext（携带模型配置，供 Planner/Executor/Synthesizer 调用 LLM）
                AgentContext context = AgentContext.builder()
                        .sessionId(sessionId)
                        .originalQuestion(questionForm.getQuestion())
                        .datasetId(questionForm.getDatasetId())
                        .modelId(questionForm.getModelId())
                        .model(modelConfigBean.getModel())
                        .modelType(modelConfigBean.getType())
                        .apiKey(modelConfigBean.getApiKey())
                        .baseUrl(modelConfigBean.getBaseUrl())
                        .platform(modelConfigBean.getPlatform())
                        .userId(SecurityUtil.getCurrentUserId())
                        .build();

                // 发送开始事件
                sink.tryEmitNext(ServerSentEvent.<WebResult<String>>builder()
                        .event("attribution_start")
                        .data(WebResult.success("开始归因分析，问题: " + questionForm.getQuestion()))
                        .build());

                // 心跳线程：每隔 10 秒发送进度事件，防止 SSE 连接超时断开
                Thread heartbeatThread = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            Thread.sleep(10000);
                            sink.tryEmitNext(ServerSentEvent.<WebResult<String>>builder()
                                    .event("progress")
                                    .data(WebResult.success("归因分析进行中..."))
                                    .build());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                });
                heartbeatThread.setDaemon(true);
                heartbeatThread.start();

                // 执行本地归因分析（Planner → Executor → Synthesizer 直连，无 Redis）
                AttributionReport report = agentOrchestrator.analyze(context, progressMsg -> {
                    sink.tryEmitNext(ServerSentEvent.<WebResult<String>>builder()
                            .event("progress")
                            .data(WebResult.success(progressMsg))
                            .build());
                });

                // 停止心跳
                heartbeatThread.interrupt();

                // 返回 Markdown 格式报告
                String markdown = report.toMarkdown();
                sink.tryEmitNext(ServerSentEvent.<WebResult<String>>builder()
                        .event("attribution_report")
                        .data(WebResult.success(markdown))
                        .build());

                // 更新 question_log
                QuestionLogBean updateLogBean = QuestionLogBean.builder()
                        .questionId(questionForm.getQuestionId())
                        .sessionId(sessionId)
                        .answer(markdown)
                        .costToken((long) report.getTotalTokenUsage())
                        .costTime(String.valueOf(report.getTotalExecutionTimeMs()))
                        .build();
                questionLogMapper.update(updateLogBean,
                        new LambdaQueryWrapper<QuestionLogBean>()
                                .eq(QuestionLogBean::getQuestionId, questionForm.getQuestionId())
                                .eq(QuestionLogBean::getSessionId, sessionId));

                sink.tryEmitNext(ServerSentEvent.<WebResult<String>>builder()
                        .event("complete")
                        .data(WebResult.success("[DONE]"))
                        .build());
                sink.tryEmitComplete();

            } catch (Exception e) {
                log.error("归因分析执行失败", e);
                sink.tryEmitNext(ServerSentEvent.<WebResult<String>>builder()
                        .event("error")
                        .data(WebResult.error(500, "归因分析执行异常: " + e.getMessage()))
                        .build());
                sink.tryEmitComplete();
            }
        });

        return sink.asFlux();
    }
}
