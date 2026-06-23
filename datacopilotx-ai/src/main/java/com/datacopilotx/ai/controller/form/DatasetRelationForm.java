package com.datacopilotx.ai.controller.form;

import lombok.Data;

import java.util.List;

@Data
public class DatasetRelationForm {

    @Data
    public static class Create {
        private Long id;
        private Long fromDatasetId;
        private String fromDatasetName;
        private String fromField;
        private Long toDatasetId;
        private String toDatasetName;
        private String toField;
        private String relationType;
        private String description;
    }

    @Data
    public static class Delete {
        private Long id;
    }
}