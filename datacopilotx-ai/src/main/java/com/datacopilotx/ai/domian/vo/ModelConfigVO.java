package com.datacopilotx.ai.domian.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

public class ModelConfigVO {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class List {
        private Long id;
        private String model;
        private String type;
        private String platform;
        private String functionType;
        private Boolean enable;
        private String apiKey;
        private String apiBase;
        private Integer dimension;
        private String createTime;
    }
}
