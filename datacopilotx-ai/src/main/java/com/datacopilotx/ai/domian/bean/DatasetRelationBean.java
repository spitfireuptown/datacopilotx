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
    
    @TableField("from_dataset_id")
    private Long fromDatasetId;
    
    @TableField("from_dataset_name")
    private String fromDatasetName;
    
    @TableField("from_field")
    private String fromField;
    
    @TableField("to_dataset_id")
    private Long toDatasetId;
    
    @TableField("to_dataset_name")
    private String toDatasetName;
    
    @TableField("to_field")
    private String toField;
    
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