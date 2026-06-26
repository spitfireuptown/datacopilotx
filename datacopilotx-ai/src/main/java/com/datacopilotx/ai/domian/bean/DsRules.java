package com.datacopilotx.ai.domian.bean;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
@TableName("DS_RULES")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DsRules implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("enable")
    private Integer enable;

    @TableField("name")
    private String name;

    @TableField("permission_list")
    private String permissionList;

    @TableField("user_list")
    private String userList;

    @TableField("creator")
    private String creator;

    @TableField("is_del")
    @TableLogic
    private Integer isDel;

    @TableField("ctime")
    private Timestamp ctime;

    @TableField("utime")
    private Timestamp utime;
}