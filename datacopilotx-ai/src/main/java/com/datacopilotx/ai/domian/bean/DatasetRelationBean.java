package com.datacopilotx.ai.domian.bean;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
@TableName("DATA_SET_RELATION")
@AllArgsConstructor
@NoArgsConstructor
public class DatasetRelationBean implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("dataset_id")
    private Long datasetId;
    
    @TableField("left_table")
    private String leftTable;
    
    @TableField("left_field")
    private String leftField;
    
    @TableField("right_table")
    private String rightTable;
    
    @TableField("right_field")
    private String rightField;
    
    @TableField("relation_type")
    private String relationType;
    
    @TableField("description")
    private String description;
    
    private Timestamp utime;
    
    private Timestamp ctime;
    
    @TableField("is_del")
    @TableLogic
    private Integer isDel;
    
    @TableField("creator")
    private String creator;
}