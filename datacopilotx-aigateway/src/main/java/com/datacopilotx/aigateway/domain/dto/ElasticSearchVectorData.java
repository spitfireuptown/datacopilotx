package com.datacopilotx.aigateway.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.ObjectUtils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ElasticSearchVectorData {
    private String docId;
    // 数据集id
    private Long datasetId;
    // 模型id
    private Long modelId;
    // 问题
    private String question;
    // 问题关键字（用于精确匹配）
    private String question_kw;
    // 召回替换文案
    private String answer;
    // 答案关键字（用于精确匹配）
    private String answer_kw;
    private Boolean enable;
    private Timestamp ctime;
    private String createTime;
    private List<Float> vector;
    public String getCreateTime() {

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return ObjectUtils.isEmpty(this.ctime) ? "" : df.format(new Date(this.ctime.getTime()));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ElasticSearchVectorPageDTO  {
        @Builder.Default
        private List<ElasticSearchVectorData> data = List.of();
        private Long total;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ElasticSearchFormDTO {
        private Integer pageNo;
        private Integer pageSize;
        private String indexName;
        private String name;
        private Boolean enable;
    }
}
