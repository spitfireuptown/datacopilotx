package com.datacopilotx.ai.domian.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: uptown
 * @date: 2025/8/31 15:23
 */
public class DataSetDTO {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class SchemaInfo {
        private String fieldName;
        private String fieldType;
        private String description;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class DriverInfo {
        private String host;
        private Long port;
        private String database;
        private String table;
        private String username;
        private String password;
        private String type;
    }
}
