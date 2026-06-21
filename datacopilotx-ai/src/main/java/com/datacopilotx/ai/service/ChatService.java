package com.datacopilotx.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datacopilotx.ai.controller.form.QuestionForm;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.ModelConfigBean;
import com.datacopilotx.ai.domian.bean.QuestionLogBean;
import com.datacopilotx.ai.mapper.DataSetMapper;
import com.datacopilotx.ai.mapper.ModelConfigMapper;
import com.datacopilotx.ai.mapper.QuestionLogMapper;
import com.datacopilotx.ai.service.flow.*;
import com.datacopilotx.ai.service.graph.main.SerializableSink;
import com.datacopilotx.ai.service.graph.main.WorkflowGraph;
import com.datacopilotx.ai.service.graph.main.WorkflowState;
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
import org.springframework.util.ObjectUtils;
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

        QuestionLogBean questionLogBean = QuestionLogBean
                .builder()
                .questionId(questionId)
                .sessionId(sessionId)
                .dataId(questionForm.getDatasetId())
                .question(questionForm.getQuestion())
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
                        questionForm.getQuestion()
                );

                // 将 sink、dataSetBean、modelConfigBean 放入 initialData
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
                StringBuilder answerBuilder = new StringBuilder();

                for (var nodeOutput : compiledGraph.stream(initialData, runnableConfig)) {
                    nodeCount++;
                    finalState = nodeOutput.state();

                    // 将每一步的回答追加到 answerBuilder
                    finalState.answer().ifPresent(answerContent -> {
                        if (answerContent != null && !answerContent.isEmpty()) {
                            if (answerBuilder.length() > 0) {
                                answerBuilder.append("\n\n");
                            }
                            answerBuilder.append(answerContent);
                        }
                    });

                    log.debug("Executed node {}, current state: {}", nodeCount, finalState);
                }

                if (finalState == null) {
                    throw new IllegalStateException("Workflow execution did not return any state");
                }
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
}
