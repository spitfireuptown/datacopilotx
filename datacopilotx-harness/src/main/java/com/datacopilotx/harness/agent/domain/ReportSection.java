package com.datacopilotx.harness.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 报告章节 —— 对应一个子任务分析维度的归因结论
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSection {

    /** 章节标题 */
    private String title;

    /** 章节内容（Markdown 格式） */
    private String content;

    /** 关联的子任务 ID 列表 */
    @Builder.Default
    private List<String> sourceTaskIds = new ArrayList<>();

    /** 归因分析角度 */
    private String attributionAngle;

    /** 置信度（0-1） */
    @Builder.Default
    private double confidence = 1.0;

    /** 结构化数据（用于图表渲染） */
    private Map<String, Object> chartData;

    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(title).append("\n\n");
        if (attributionAngle != null) {
            sb.append("*分析角度：").append(attributionAngle).append("*\n\n");
        }
        sb.append(content).append("\n");
        return sb.toString();
    }
}