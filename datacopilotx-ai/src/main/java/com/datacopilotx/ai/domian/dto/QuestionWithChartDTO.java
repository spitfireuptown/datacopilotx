package com.datacopilotx.ai.domian.dto;

import lombok.Data;

@Data
public class QuestionWithChartDTO {
    private Long id;
    private String questionId;
    private String sessionId;
    private String question;
    private String result;
    private String sql;
    private String ctime;
}
