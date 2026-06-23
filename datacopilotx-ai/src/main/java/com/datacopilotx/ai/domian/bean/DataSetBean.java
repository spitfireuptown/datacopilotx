package com.datacopilotx.ai.domian.bean;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
@TableName("DATA_SET")
@AllArgsConstructor
@NoArgsConstructor
public class DataSetBean implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String host;
    @TableField("ds_name")
    private String dsName;
    @TableField("inject_prompt")
    private String injectPrompt;
    private String description;
    private Long port;
    @TableField("`username`")
    private String username;
    @TableField("`password`")
    private String password;
    @TableField("`database`")
    private String database;
    @TableField("`table`")
    private String table;
    private String fields;
    private String type;
    private Timestamp utime;
    private Timestamp ctime;
    @TableField("is_del")
    @TableLogic
    private Integer isDel;
    private String relations;
}
