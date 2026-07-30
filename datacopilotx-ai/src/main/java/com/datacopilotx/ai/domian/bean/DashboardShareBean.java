package com.datacopilotx.ai.domian.bean;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
@TableName("DASHBOARD_SHARE")
public class DashboardShareBean {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("dashboard_id")
    private Long dashboardId;

    /** 免密访问令牌（SecureRandom 256-bit，Base64URL） */
    private String token;

    /** 过期时间 */
    @TableField("expire_time")
    private Timestamp expireTime;

    private String creator;

    @TableField("is_del")
    @TableLogic
    private Integer isDel;

    private Timestamp ctime;

    private Timestamp utime;
}
