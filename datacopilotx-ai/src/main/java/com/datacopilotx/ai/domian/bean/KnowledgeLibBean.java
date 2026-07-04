package com.datacopilotx.ai.domian.bean;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * 知识库
 */
@Data
@TableName("KNOWLEDGE_LIB")
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeLibBean {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    // 知识库名称
    private String name;
    // 知识库绑定的数据集ID，一个数据集可以绑定多个知识库
    @TableField(value = "dataset_id")
    private Long datasetId;
    // 知识库绑定的模型ID，用于做嵌入
    @TableField(value = "model_id")
    private Long modelId;
    // 知识库描述
    private String description;
    private Timestamp utime;
    private Timestamp ctime;
    @TableField("is_del")
    @TableLogic
    private Integer isDel;
    
    @TableField("creator")
    private String creator;
    
    @TableField("score")
    private Float score;
}
