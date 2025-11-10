package com.datacopilotx.ai.controller.form;

import com.datacopilotx.ai.domian.dto.ModelConfigDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class ModelForm {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Create {
        private Long id;
        private String model;
        private String apiKey;
        private String apiBase;
        private String platform;
        private String type;
        private String functionType;
        private Integer dimension;
    }
}
