package com.datacopilotx.aigateway.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OllamaResultDTO {
    private String model;
    private String createAt;
    private MessageDTO message;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class MessageDTO {
        private String role;
        private String content;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class EmbeddingResultDTO {
        private List<List<Float>> embeddings;
        private List<Float> embedding;
        private String model;
    }

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class CallBackResult {
        private Double score;
        private String question;
        private String answer;
    }
}
