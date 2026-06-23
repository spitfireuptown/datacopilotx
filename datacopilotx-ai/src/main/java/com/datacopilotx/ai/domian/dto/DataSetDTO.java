package com.datacopilotx.ai.domian.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: uptown
 * @date: 2025/8/31 15:23
 */
public class DataSetDTO {

    @Builder
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

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class ExcelDataSetInfo {
        private List<List<String>> context;
        private List<String> headers;
    }

    /**
     * 表关联关系
     */
    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class TableRelation {
        private String fromTable;
        private String fromField;
        private String toTable;
        private String toField;
        private String relationType;
    }
}
