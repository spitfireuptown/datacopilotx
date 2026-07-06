package com.datacopilotx.ai.domian.bean;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
@TableName("DATA_TABLE")
@AllArgsConstructor
@NoArgsConstructor
public class DataTableBean implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("dataset_id")
    private Long datasetId;
    @TableField("`table`")
    private String table;
    private String fields;
    @TableField("inject_prompt")
    private String injectPrompt;
    @TableField("embedding")
    private String embedding;
    private Timestamp utime;
    private Timestamp ctime;
    @TableField("is_del")
    @TableLogic
    private Integer isDel;
}