package com.datacopilotx.ai.service.graph.main;

import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.ModelConfigBean;
import com.datacopilotx.ai.util.SecurityUtil;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.common.exception.DataCopilotXException;
import com.datacopilotx.common.result.WebResult;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WorkflowState extends AgentState {

    public static final Map<String, Channel<?>> SCHEMA = Map.ofEntries(
            Map.entry("session_id", Channels.base(null, null)),
            Map.entry("question_id", Channels.base(null, null)),
            Map.entry("dataset_id", Channels.base(null, null)),
            Map.entry("model_id", Channels.base(null, null)),
            Map.entry("question", Channels.base(null, null)),
            Map.entry("beautiful_question", Channels.base(null, null)),
            Map.entry("token", Channels.base(null, null)),
            Map.entry("answer", Channels.base(null, null)),
            Map.entry("sql", Channels.base(null, null)),
            Map.entry("result", Channels.base(null, null)),
            Map.entry("intent_score", Channels.base(null, null)),
            Map.entry("intent_analysis", Channels.base(null, null)),
            Map.entry("recall", Channels.appender(List::of)),
            Map.entry("sql_error", Channels.base(null, null)),
            Map.entry("retry_count", Channels.base(null, null)),
            Map.entry("user_id", Channels.base(null, null)),
            Map.entry("user_role", Channels.base(null, null)),
            Map.entry("is_admin", Channels.base(null, null)),
            Map.entry("is_piaoyitong_user", Channels.base(null, null))
    );

    public WorkflowState(Map<String, Object> initData) {
        super(initData);
        this.serializableSink = (SerializableSink) initData.get("sink");
        this.dataSetBean = (DataSetBean) initData.get("data_set_bean");
        this.modelConfigBean = (ModelConfigBean) initData.get("model_config_bean");
    }

    public Optional<String> sessionId() {
        return this.value("session_id");
    }

    public Optional<String> questionId() {
        return this.value("question_id");
    }

    public Optional<Long> datasetId() {
        return this.value("dataset_id");
    }

    public Optional<Long> modelId() {
        return this.value("model_id");
    }

    public Optional<String> question() {
        return this.value("question");
    }

    public Optional<String> beautifulQuestion() {
        return this.value("beautiful_question");
    }

    public Optional<Integer> token() {
        return this.value("token");
    }

    public Optional<String> answer() {
        return this.value("answer");
    }

    public Optional<String> sql() {
        return this.value("sql");
    }

    public Optional<String> result() {
        return this.value("result");
    }

    public Optional<Integer> intentScore() {
        return this.value("intent_score");
    }

    public Optional<String> intentAnalysis() {
        return this.value("intent_analysis");
    }

    @SuppressWarnings("unchecked")
    public List<String> recall() {
        return this.<List<String>>value("recall").orElse(List.of());
    }

    public Optional<String> sqlError() {
        return this.value("sql_error");
    }

    public Optional<Integer> retryCount() {
        return this.value("retry_count");
    }

    public Optional<String> userId() {
        return this.value("user_id");
    }

    public Optional<Integer> userRole() {
        return this.value("user_role");
    }

    public Optional<Boolean> isAdmin() {
        return this.value("is_admin");
    }

    public Optional<Boolean> isPiaoyitongUser() {
        return this.value("is_piaoyitong_user");
    }
    
    // 使用 ThreadLocal 存储收集的数据，避免序列化问题
    private static final ThreadLocal<StringBuilder> collectedDataHolder = new ThreadLocal<>();
    
    public static void initCollectedData() {
        collectedDataHolder.set(new StringBuilder());
    }
    
    public void appendCollectedData(String data) {
        StringBuilder sb = collectedDataHolder.get();
        if (sb != null && data != null) {
            sb.append(data);
        }
    }
    
    public String getCollectedData() {
        StringBuilder sb = collectedDataHolder.get();
        return sb != null ? sb.toString() : "";
    }
    
    public void clearCollectedData() {
        collectedDataHolder.remove();
    }

    private transient ModelConfigBean modelConfigBean;
    private transient DataSetBean dataSetBean;
    private transient SerializableSink serializableSink;

    public ModelConfigBean getModelConfigBean() {
        return modelConfigBean;
    }

    public DataSetBean getDataSetBean() {
        return dataSetBean;
    }

    public Sinks.Many<ServerSentEvent<WebResult<String>>> getSink() {
        return serializableSink != null ? serializableSink.getSink() : null;
    }
    
    public SerializableSink getSerializableSink() {
        return serializableSink;
    }

    public ChatRequest buildLLMRequest(String question) {
        ModelConfigBean modelConfigBean = this.getModelConfigBean();
        if (question == null) {
            throw new DataCopilotXException("question is null");
        }

        return ChatRequest.builder()
                .model(modelConfigBean.getModel())
                .platform(modelConfigBean.getPlatform())
                .type(modelConfigBean.getType())
                .question(question)
                .apiKey(modelConfigBean.getApiKey())
                .baseUrl(modelConfigBean.getBaseUrl())
                .build();
    }

    public ChatRequest buildLLMRequest() {
        String beautifulQuestion = this.beautifulQuestion().orElse(null);
        return this.buildLLMRequest(beautifulQuestion);
    }

    public static Map<String, Object> createInitialState(
            String sessionId,
            String questionId,
            Long datasetId,
            Long modelId,
            String question) {
        String userId = SecurityUtil.getCurrentUserId();
        Integer userRole = SecurityUtil.getCurrentUserRole();
        boolean isAdmin = SecurityUtil.isAdmin();
        boolean isPiaoyitongUser = userId != null && !isAdmin;

        return Map.of(
                "session_id", sessionId,
                "question_id", questionId,
                "dataset_id", datasetId,
                "model_id", modelId,
                "question", question,
                "user_id", userId != null ? userId : "",
                "user_role", userRole != null ? userRole : 2,
                "is_admin", isAdmin,
                "is_piaoyitong_user", isPiaoyitongUser
        );
    }
}