package com.datacopilotx.ai.service.graph;

import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.ModelConfigBean;
import com.datacopilotx.ai.domian.bean.QuestionLogBean;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.common.exception.DataCopilotXException;
import com.datacopilotx.common.result.WebResult;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

/**
 * Deep research state model
 * 
 * @author imfangs
 */
@Slf4j
public class WorkflowState extends AgentState {

    /**
     * State schema definition
     */
    public static final Map<String, Channel<?>> SCHEMA = Map.ofEntries(
        Map.entry("session_id", Channels.base(null, null)),
        Map.entry("question_id", Channels.base(null, null)),
        Map.entry("dataset_id", Channels.base(null, null)),
        Map.entry("model_id", Channels.appender(() -> new ArrayList<String>())),
        Map.entry("question", Channels.appender(() -> new ArrayList<String>())),
        Map.entry("beautiful_question", Channels.appender(() -> new ArrayList<String>())),
        Map.entry("token", Channels.base(null, null)),
        Map.entry("time_cost", Channels.base(null, null)),
        Map.entry("model", Channels.base(null, null)),
        Map.entry("platform", Channels.base(null, null)),
        Map.entry("answer", Channels.base(null, null)),
        Map.entry("sql", Channels.base(null, null)),
        Map.entry("sink", Channels.base(null, null)),
        Map.entry("model_config_bean", Channels.base(null, null)),
        Map.entry("data_set_bean", Channels.base(null, null)),
        Map.entry("question_log_bean", Channels.base(null, null)),

        Map.entry("intent_score", Channels.base(null, null)),
        Map.entry("intent_analysis", Channels.base(null, null)),
        Map.entry("recall", Channels.base(null, null))
    );

    public Optional<String> recall() {
        return this.value("recall");
    }

    public WorkflowState(Map<String, Object> initData) {
        super(initData);
    }

    public Optional<String> intentAnalysis() {
        return this.value("intent_analysis");
    }

    public Optional<String> intentScore() {
        return this.value("intent_score");
    }

    public Optional<String> sessionId() {
        return this.value("session_id");
    }

    public Optional<String> questionId() {
        return this.value("question_id");
    }

    public Optional<String> datasetId() {
        return this.value("dataset_id");
    }

    public Optional<String> modelId() {
        return this.value("model_id");
    }

    public Optional<String> question() {
        return this.value("question");
    }

    public Optional<String> beautifulQuestion() {
        return this.value("beautiful_question");
    }
    public Optional<String> token() {
        return this.value("token");
    }

    public Optional<LocalDateTime> timeCost() {
        return this.value("time_cost");
    }

    public Optional<String> model() {
        return this.value("model");
    }

    public Optional<String> platform() {
        return this.value("platform");
    }

    public Optional<String> answer() {
        return this.value("answer");
    }

    public Optional<String> sql() {
        return this.value("sql");
    }

    public Optional<Sinks.Many<ServerSentEvent<WebResult<String>>>> sink() {
        return this.value("sink");
    }

    public Optional<ModelConfigBean> modelConfigBean() {
        return this.value("model_config_bean");
    }

    public Optional<DataSetBean> dataSetBean() {
        return this.value("data_set_bean");
    }

    public Optional<QuestionLogBean> questionLogBean() {
        return this.value("question_log_bean");
    }

    /**
     * Create initial state
     */
    public static Map<String, Object> createInitialState(
            String sessionId,
            String questionId,
            String datasetId,
            Integer modelId,
            Sinks.Many<ServerSentEvent<WebResult<String>>> sink) {
        
        return Map.of(
            "session_id", sessionId,
            "question_id", questionId,
            "dataset_id", datasetId,
            "model_id", modelId,
            "sink", sink
        );
    }


    public ChatRequest buildLLMRequest() {
        ModelConfigBean modelConfigBean = this.modelConfigBean().orElseThrow(() ->new DataCopilotXException("model_config_bean is null"));
        String beautifulQuestion = this.beautifulQuestion().orElseThrow(() ->new DataCopilotXException("beautiful_question is null"));

        return ChatRequest.builder()
                .model(modelConfigBean.getModel())
                .platform(modelConfigBean.getPlatform())
                .type(modelConfigBean.getType())
                .question(beautifulQuestion)
                .apiKey(modelConfigBean.getApiKey())
                .baseUrl(modelConfigBean.getBaseUrl())
                .build();
    }
}