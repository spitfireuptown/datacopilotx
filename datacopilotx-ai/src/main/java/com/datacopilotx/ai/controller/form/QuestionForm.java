package com.datacopilotx.ai.controller.form;

import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.ModelConfigBean;
import com.datacopilotx.ai.domian.bean.QuestionLogBean;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class QuestionForm {
    private String sessionId;
    private String questionId;
    private Long datasetId;
    private Long modelId;
    private String question;
    private String beautifulQuestion;
    private Integer token;
    private Long timeCost;
    private String model;
    private String platform;
    private String answer;
    private String sql;
    private List<HistoryItem> history;

    // 模型配置
    private ModelConfigBean modelConfigBean;
    // 数据源配置
    private DataSetBean dataSetBean;
    // 请求日志
    private QuestionLogBean questionLogBean;

    public ChatRequest buildLLMRequest() {
        return ChatRequest.builder()
                .model(this.modelConfigBean.getModel())
                .platform(this.modelConfigBean.getPlatform())
                .type(this.modelConfigBean.getType())
                .question(this.getBeautifulQuestion())
                .apiKey(this.modelConfigBean.getApiKey())
                .baseUrl(this.modelConfigBean.getBaseUrl())
                .build();
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class HistoryItem {
        private String role;
        private String content;
    }
}
