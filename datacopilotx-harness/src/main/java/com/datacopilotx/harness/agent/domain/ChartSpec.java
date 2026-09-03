package com.datacopilotx.harness.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 图表规格 —— ChartAnalystAgent 从执行数据中提取的可视化图表与解释
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartSpec {

    /** 图表标题 */
    private String title;

    /** 图表类型：line / bar / pie */
    private String chartType;

    /** 横轴字段含义 */
    private String xField;

    /** 纵轴字段含义 */
    private String yField;

    /** 数据点列表 */
    @Builder.Default
    private List<ChartDataPoint> data = new ArrayList<>();

    /** 图表解释（自然语言洞察） */
    private String explanation;

    /**
     * 图表数据点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartDataPoint {
        /** 分类 / 时间标签 */
        private String label;
        /** 数值 */
        private double value;
    }
}
