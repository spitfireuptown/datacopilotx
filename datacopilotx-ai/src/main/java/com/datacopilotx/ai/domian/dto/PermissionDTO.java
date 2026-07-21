package com.datacopilotx.ai.domian.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PermissionDTO {

    private Long id;

    private Integer enable;

    private String type;

    private Long dsId;

    private String dsName;

    private Long tableId;

    private String tableName;

    private String name;

    private ExpressionTree expressionTree;

    private List<ColumnPermission> permissions;

    private List<String> whiteListUser;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExpressionTree {
        private String logic;
        private List<ExpressionItem> items;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExpressionItem {
        private String type;
        private Long fieldId;
        private String fieldName;
        private String filterType;
        private String term;
        private Object value;
        private ExpressionTree subTree;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ColumnPermission {
        private Long fieldId;
        private String fieldName;
        private String fieldComment;
        private Boolean enable;
    }
}