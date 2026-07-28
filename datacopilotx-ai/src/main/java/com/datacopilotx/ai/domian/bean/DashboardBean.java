package com.datacopilotx.ai.domian.bean;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
@TableName("DASHBOARD")
public class DashboardBean {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String creator;

    @TableField("is_del")
    @TableLogic
    private Integer isDel;

    private Timestamp ctime;

    private Timestamp utime;
}
