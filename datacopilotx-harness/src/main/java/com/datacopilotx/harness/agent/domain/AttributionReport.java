package com.datacopilotx.harness.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 归因分析报告 —— Synthesizer 综合所有子任务结果后生成的最终报告
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributionReport {

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

    /** 报告章节列表 */
    @Builder.Default
    private List<ReportSection> sections = new ArrayList<>();

    /** 关键发现 —— 归因分析的核心结论 */
    @Builder.Default
    private List<String> keyFindings = new ArrayList<>();

    /** 建议与行动项 */
    @Builder.Default
    private List<String> recommendations = new ArrayList<>();

    /**
     * 将报告格式化为 Markdown 字符串
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(title != null ? title : "归因分析报告").append("\n\n");
        sb.append("> 原始问题：").append(originalQuestion).append("\n\n");
        sb.append("## 执行摘要\n\n").append(executiveSummary).append("\n\n");

        if (!keyFindings.isEmpty()) {
            sb.append("## 关键发现\n\n");
            for (int i = 0; i < keyFindings.size(); i++) {
                sb.append(i + 1).append(". ").append(keyFindings.get(i)).append("\n");
            }
            sb.append("\n");
        }

        for (ReportSection section : sections) {
            sb.append(section.toMarkdown()).append("\n");
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