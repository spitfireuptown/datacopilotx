package com.datacopilotx.ai.controller.form;

import lombok.Data;

import java.util.List;

@Data
public class DatasetRelationForm {

    @Data
    public static class Create {
        private Long id;
        private Long datasetId;
        private String leftTable;
        private String leftField;
        private String rightTable;
        private String rightField;
        private String relationType;
        private String description;
    }

    @Data
    public static class Delete {
        private Long id;
    }
}