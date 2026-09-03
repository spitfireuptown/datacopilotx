package com.datacopilotx.harness.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据预测结果 —— PredictorAgent 基于历史数据与归因结论生成的预测
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResult {

    /** 趋势数据点（历史点 + 预测点，按时间顺序排列） */
    @Builder.Default
    private List<TrendPoint> trendPoints = new ArrayList<>();

    /** 核心指标预测 */
    @Builder.Default
    private List<ForecastMetric> metrics = new ArrayList<>();

    /** 预测结论（Markdown 格式） */
    private String forecastSummary;

    /** 置信水平：高/中/低 */
    private String confidenceLevel;

    /** 风险提示列表 */
    @Builder.Default
    private List<String> risks = new ArrayList<>();

    /** 是否生成成功（false 时前端展示降级占位） */
    @Builder.Default
    private boolean success = true;

    /**
     * 趋势数据点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        /** 横轴标签（时间或周期） */
        private String label;
        /** 数值 */
        private double value;
        /** 是否为预测点（true 为预测点，前端以虚线渲染） */
        @Builder.Default
        private boolean isForecast = false;
    }

    /**
     * 核心指标预测
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastMetric {
        /** 指标名称 */
        private String name;
        /** 当前值 */
        private double currentValue;
        /** 预测值 */
        private double forecastValue;
        /** 变化率（如 0.2 表示 +20%） */
        private double changeRate;
    }
}
