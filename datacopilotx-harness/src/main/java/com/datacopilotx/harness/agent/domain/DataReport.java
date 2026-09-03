package com.datacopilotx.harness.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据报告 —— 归因分析内容 + 数据预测内容 + 图表解释的完整报告
 * <p>
 * 由 AgentOrchestrator.generateReport 组装：AttributionReport（归因）+ PredictionResult（预测）+ ChartSpec（图表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataReport {

    /** 报告唯一标识 */
    private String reportId;

    /** 原始问题 */
    private String originalQuestion;

    /** 报告标题 */
    private String title;

    /** 报告生成时间戳 */
    private long createdAt;

    /** 总执行耗时（毫秒） */
    private long totalExecutionTimeMs;

    /** 总 Token 消耗 */
    private int totalTokenUsage;

    /** 执行摘要 —— 总体结论 */
    private String executiveSummary;

    /** 关键发现 */
    @Builder.Default
    private List<String> keyFindings = new ArrayList<>();

    /** 归因分析章节列表（来自 AttributionReport） */
    @Builder.Default
    private List<ReportSection> sections = new ArrayList<>();

    /** 数据预测结果 */
    private PredictionResult prediction;

    /** 图表解释列表 */
    @Builder.Default
    private List<ChartSpec> charts = new ArrayList<>();

    /** 建议与行动项 */
    @Builder.Default
    private List<String> recommendations = new ArrayList<>();

    /**
     * 将报告格式化为 Markdown 字符串（供入库与降级展示）
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(title != null ? title : "数据分析报告").append("\n\n");
        sb.append("> 原始问题：").append(originalQuestion).append("\n\n");
        sb.append("## 执行摘要\n\n").append(executiveSummary).append("\n\n");

        if (!keyFindings.isEmpty()) {
            sb.append("## 关键发现\n\n");
            for (int i = 0; i < keyFindings.size(); i++) {
                sb.append(i + 1).append(". ").append(keyFindings.get(i)).append("\n");
            }
            sb.append("\n");
        }

        if (!sections.isEmpty()) {
            sb.append("## 归因分析\n\n");
            for (ReportSection section : sections) {
                sb.append(section.toMarkdown()).append("\n");
            }
        }

        if (prediction != null && prediction.isSuccess()) {
            sb.append("## 数据预测\n\n");
            if (prediction.getForecastSummary() != null) {
                sb.append(prediction.getForecastSummary()).append("\n\n");
            }
            if (!prediction.getMetrics().isEmpty()) {
                sb.append("| 指标 | 当前值 | 预测值 | 变化率 |\n|---|---|---|---|\n");
                for (PredictionResult.ForecastMetric metric : prediction.getMetrics()) {
                    sb.append("| ").append(metric.getName())
                            .append(" | ").append(metric.getCurrentValue())
                            .append(" | ").append(metric.getForecastValue())
                            .append(" | ").append(String.format("%.1f%%", metric.getChangeRate() * 100))
                            .append(" |\n");
                }
                sb.append("\n");
            }
            if (prediction.getConfidenceLevel() != null) {
                sb.append("*置信水平：").append(prediction.getConfidenceLevel()).append("*\n\n");
            }
            if (!prediction.getRisks().isEmpty()) {
                sb.append("**风险提示：**\n\n");
                for (String risk : prediction.getRisks()) {
                    sb.append("- ").append(risk).append("\n");
                }
                sb.append("\n");
            }
        }

        if (!charts.isEmpty()) {
            sb.append("## 图表解释\n\n");
            for (ChartSpec chart : charts) {
                sb.append("### ").append(chart.getTitle()).append("\n\n");
                if (!chart.getData().isEmpty()) {
                    sb.append("| ").append(chart.getXField()).append(" | ").append(chart.getYField()).append(" |\n|---|---|\n");
                    for (ChartSpec.ChartDataPoint point : chart.getData()) {
                        sb.append("| ").append(point.getLabel()).append(" | ").append(point.getValue()).append(" |\n");
                    }
                    sb.append("\n");
                }
                if (chart.getExplanation() != null) {
                    sb.append(chart.getExplanation()).append("\n\n");
                }
            }
        }

        if (!recommendations.isEmpty()) {
            sb.append("## 建议与行动项\n\n");
            for (int i = 0; i < recommendations.size(); i++) {
                sb.append(i + 1).append(". ").append(recommendations.get(i)).append("\n");
            }
            sb.append("\n");
        }

        sb.append("---\n");
        sb.append("*报告生成耗时：").append(totalExecutionTimeMs).append("ms | Token消耗：").append(totalTokenUsage).append("*\n");
        return sb.toString();
    }
}
