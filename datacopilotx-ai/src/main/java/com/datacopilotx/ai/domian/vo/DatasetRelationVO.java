package com.datacopilotx.ai.domian.vo;

import lombok.Data;

import java.sql.Timestamp;

public class DatasetRelationVO {

    @Data
    public static final class ListVO {
        private Long id;
        private Long fromDatasetId;
        private String fromDatasetName;
        private String fromField;
        private Long toDatasetId;
        private String toDatasetName;
        private String toField;
        private String relationType;
        private String description;
        private String createTime;
    }

    @Data
    public static final class DetailVO {
        private Long id;
        private Long fromDatasetId;
        private String fromDatasetName;
        private String fromField;
        private Long toDatasetId;
        private String toDatasetName;
        private String toField;
        private String relationType;
        private String description;
        private Timestamp ctime;
        private Timestamp utime;
    }
}