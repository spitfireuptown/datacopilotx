package com.datacopilotx.ai.domian.bean;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
@TableName("DASHBOARD_CHART")
public class DashboardChartBean {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("dashboard_id")
    private Long dashboardId;

    @TableField("chart_name")
    private String chartName;

    @TableField("chart_type")
    private String chartType;

    @TableField("chart_data")
    private String chartData;

    @TableField("sql_text")
    private String sqlText;

    private String question;

    @TableField("question_id")
    private String questionId;

    @TableField("session_id")
    private String sessionId;

    @TableField("layout_x")
    private Integer layoutX;

    @TableField("layout_y")
    private Integer layoutY;

    @TableField("layout_w")
    private Integer layoutW;

    @TableField("layout_h")
    private Integer layoutH;

    private String creator;

    @TableField("is_del")
    @TableLogic
    private Integer isDel;

    private Timestamp ctime;

    private Timestamp utime;
}
