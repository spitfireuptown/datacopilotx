package com.datacopilotx.ai.domian.bean;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.sql.Timestamp;

@Data
@Builder
@TableName("QUESTION_LOG")
public class QuestionLogBean {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("question_id")
    private String questionId;
    @TableField("session_id")
    private String sessionId;
    @TableField("dataset_id")
    private Long datasetId;
    @TableField("model_id")
    private Long modelId;
    private String question;
    @TableField("cost_time")
    private String costTime;
    @TableField("cost_token")
    private Long costToken;
    @TableField("`sql`")
    private String sql;
    private String answer;
    private String result;
    private Timestamp utime;
    private Timestamp ctime;
    @TableField("is_del")
    @TableLogic
    private Integer isDel;
    @TableField("creator")  
    private String creator;
}
