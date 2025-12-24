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
import com.datacopilotx.ai.service.graph.WorkflowGraphBuilder;
import com.datacopilotx.ai.service.graph.WorkflowState;
import com.datacopilotx.common.constant.PromptConstant;
import com.datacopilotx.common.result.WebResult;
import com.datacopilotx.common.util.IdUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

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
    private GracefulQuestionStep gracefulQuestionStep;
    @Resource
    private IntentRecognitionStep intentRecognitionStep;
    @Resource
    private GenerateSqlStep generateSqlStep;
    @Resource
    private ExecuteSQLStep executeSQLStep;
    @Resource
    private RecallKnowledgeStep recallKnowledgeStep;
    @Resource
    private WorkflowServiceHelper flowServiceHelper;
    @Resource
    WorkflowGraphBuilder workflowGraphBuilder;


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

        // 构建调用链
        gracefulQuestionStep
            // 意图识别
            .setNextStep(intentRecognitionStep)
            // RAG 知识库召回
            .setNextStep(recallKnowledgeStep)
            // 生成对应数据库引擎SQL
            .setNextStep(generateSqlStep)
            // 执行SQL
            .setNextStep(executeSQLStep);

        gracefulQuestionStep.process(sink, Map.of(), questionForm);
        return sink.asFlux();
    }
}
