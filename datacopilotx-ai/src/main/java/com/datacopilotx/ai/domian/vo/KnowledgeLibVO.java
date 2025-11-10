package com.datacopilotx.ai.domian.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class KnowledgeLibVO {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class List {
        private Long id;
        private String name;
        private Long datasetId;
        private Long modelId;
        private String description;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class DataList {
        private String docId;
        private String question;
        private String answer;
        private Boolean enable;
    }
}
