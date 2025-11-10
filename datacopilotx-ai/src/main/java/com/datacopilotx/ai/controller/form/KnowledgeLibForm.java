package com.datacopilotx.ai.controller.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;

public class KnowledgeLibForm {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class PageForm {
        private Integer pageNo;
        private Integer pageSize;
        private Long knowledgeLibId;
        private String name;
        private Boolean enable;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateForm {
        private String name;
        private Long datasetId;
        private Long modelId;
        private String description;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class UpdateForm {
        private Long id;
        private String name;
        private Long datasetId;
        private Long modelId;
        private String description;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StoreForm {
        private Long knowledgeLibId;
        private String question;
        private String answer;
    }

     @EqualsAndHashCode(callSuper = true)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class UpdateDateForm extends StoreForm {
        private Boolean enable;
        private String docId;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class DeleteForm {
        private Long knowledgeLibId;
        private String docId;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class RetrievalForm {
        private Long knowledgeLibId;
        private String question;
        private Float score;
        private Integer topK;
    }
}
