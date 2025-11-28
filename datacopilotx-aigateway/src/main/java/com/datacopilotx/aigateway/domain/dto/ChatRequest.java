package com.datacopilotx.aigateway.domain.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ChatRequest {
    private String model;
    private String question;
    private String apiKey;
    private String baseUrl;
    private String platform;
    private String type;
    private String systemPrompt;
    private String userPrompt;
    private Integer tokenUsage;
    private Long timeCost;
    private Integer dimensions;
}
