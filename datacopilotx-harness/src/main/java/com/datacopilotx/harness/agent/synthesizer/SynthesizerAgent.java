package com.datacopilotx.harness.agent.synthesizer;

import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.harness.agent.context.AgentContext;
import com.datacopilotx.harness.agent.domain.AttributionReport;
import com.datacopilotx.harness.agent.domain.ExecutionResult;
import com.datacopilotx.harness.agent.domain.ReportSection;
import com.datacopilotx.harness.agent.domain.SubTask;
import com.datacopilotx.harness.agent.domain.TaskDAG;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Synthesizer（综合智能体）—— 将所有子任务结果综合为最终归因分析报告
 * <p>
 * 核心职责：
 * <ol>
 *   <li>收集所有子任务的执行结果</li>
 *   <li>调用 LLM 对结果进行归因综合分析</li>
 *   <li>生成结构化的 {@link AttributionReport} 报告</li>
 *   <li>提炼关键发现和建议</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SynthesizerAgent {

    private final AIGatewayChatService aiGatewayChatService;
    private final ObjectMapper objectMapper;

    /**
     * 综合报告生成提示词
     */
    private static final String SYNTHESIZER_SYSTEM_PROMPT = """
            你是一个归因分析报告专家，擅长将多个子任务的分析结果综合为一份结构化的归因分析报告。

            ## 你的任务
            1. 综合所有子任务的分析结果
            2. 提炼关键发现（归因结论）
            3. 生成建议与行动项
            4. 输出 Markdown 格式的完整报告

            ## 输出格式
            返回JSON：
            ```json
            {
              "title": "报告标题",
              "executiveSummary": "执行摘要",
              "keyFindings": ["发现1", "发现2"],
              "sections": [
                {
                  "title": "章节标题",
                  "content": "章节内容（Markdown）",
                  "attributionAngle": "drill_down"
                }
              ],
              "recommendations": ["建议1", "建议2"]
            }
            ```
            """;

    /**
     * 综合所有子任务结果，生成最终归因分析报告
     *
     * @param context 共享上下文（包含所有子任务执行结果）
     * @return 归因分析报告
     */
    public AttributionReport synthesize(AgentContext context) {
        log.info("[Synthesizer] 开始综合 {} 个子任务的结果",
                context.getExecutionResults().size());

        TaskDAG dag = context.getTaskDAG();
        if (dag == null) {
            log.warn("[Synthesizer] DAG 为空");
            return buildEmptyReport(context);
        }

        String synthesisPrompt = buildSynthesisPrompt(context);
        ChatRequest chatRequest = ChatRequest.builder()
                .systemPrompt(SYNTHESIZER_SYSTEM_PROMPT)
                .userPrompt(synthesisPrompt)
                .question(context.getOriginalQuestion())
                .model(context.getModel())
                .type(context.getModelType())
                .apiKey(context.getApiKey())
                .baseUrl(context.getBaseUrl())
                .platform(context.getPlatform())
                .build();

        String rawResponse = aiGatewayChatService.chatCompletions(chatRequest);
        log.debug("[Synthesizer] LLM 原始响应: {}", rawResponse);

        AttributionReport report = parseReportResponse(rawResponse, context);
        if (report == null) {
            report = buildFallbackReport(context);
        }

        report.setReportId(UUID.randomUUID().toString());
        report.setOriginalQuestion(context.getOriginalQuestion());
        report.setCreatedAt(System.currentTimeMillis());
        report.setTotalExecutionTimeMs(context.getElapsedTimeMs());
        report.setTotalTokenUsage(context.getTotalTokenUsage());

        log.info("[Synthesizer] 报告生成完成: {}", report.getTitle());
        return report;
    }

    /**
     * 构建综合分析的提示词
     */
    private String buildSynthesisPrompt(AgentContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 原始问题\n").append(context.getOriginalQuestion()).append("\n\n");

        sb.append("## 子任务及执行结果\n\n");
        TaskDAG dag = context.getTaskDAG();
        for (SubTask task : dag.getSubTasks()) {
            ExecutionResult result = context.getExecutionResult(task.getTaskId());
            sb.append("### ").append(task.getTaskId()).append(": ").append(task.getDescription()).append("\n");
            sb.append("- 类型: ").append(task.getType()).append("\n");
            if (task.getAttributionAngle() != null) {
                sb.append("- 归因角度: ").append(task.getAttributionAngle()).append("\n");
            }
            sb.append("- 是否核心归因: ").append(task.isAttributionCore()).append("\n");
            if (result != null) {
                sb.append("- 执行结果: ").append(result.getSummary()).append("\n");
                if (result.getData() != null) {
                    sb.append("- 数据: ").append(result.getData()).append("\n");
                }
            }
            sb.append("\n");
        }

        sb.append("## 要求\n");
        sb.append("1. 综合以上所有子任务结果，生成归因分析报告\n");
        sb.append("2. 重点分析标记为 attributionCore 的子任务\n");
        sb.append("3. 提炼 3-5 个关键发现\n");
        sb.append("4. 给出 2-4 条可操作建议\n");

        // 收集所有归因角度
        String angles = dag.getSubTasks().stream()
                .filter(t -> t.getAttributionAngle() != null)
                .map(t -> t.getTaskId() + "(" + t.getAttributionAngle() + ")")
                .collect(Collectors.joining(", "));
        if (!angles.isEmpty()) {
            sb.append("5. 报告章节按归因角度组织: ").append(angles).append("\n");
        }

        return sb.toString();
    }

    /**
     * 解析 LLM 返回的报告 JSON
     */
    private AttributionReport parseReportResponse(String rawResponse, AgentContext context) {
        try {
            String cleaned = com.datacopilotx.common.util.WorkflowUtil.cleanJsonStr(rawResponse);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(cleaned, Map.class);

            return AttributionReport.builder()
                    .title((String) map.getOrDefault("title", "归因分析报告"))
                    .executiveSummary((String) map.getOrDefault("executiveSummary", ""))
                    .keyFindings(parseStringList(map.get("keyFindings")))
                    .sections(parseReportSections(map.get("sections")))
                    .recommendations(parseStringList(map.get("recommendations")))
                    .build();
        } catch (Exception e) {
            log.warn("[Synthesizer] 报告解析失败: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseStringList(Object obj) {
        if (obj instanceof List) {
            return ((List<?>) obj).stream()
                    .map(Object::toString)
                    .toList();
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private List<ReportSection> parseReportSections(Object sectionsObj) {
        if (!(sectionsObj instanceof List)) {
            return new ArrayList<>();
        }
        List<ReportSection> sections = new ArrayList<>();
        for (Object item : (List<?>) sectionsObj) {
            if (item instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) item;
                sections.add(ReportSection.builder()
                        .title((String) map.getOrDefault("title", ""))
                        .content((String) map.getOrDefault("content", ""))
                        .attributionAngle((String) map.get("attributionAngle"))
                        .build());
            }
        }
        return sections;
    }

    /**
     * 构建降级报告（当 LLM 解析失败时）
     */
    private AttributionReport buildFallbackReport(AgentContext context) {
        TaskDAG dag = context.getTaskDAG();
        List<String> summaries = new ArrayList<>();
        List<ReportSection> sections = new ArrayList<>();

        if (dag != null) {
            for (SubTask task : dag.getSubTasks()) {
                ExecutionResult result = context.getExecutionResult(task.getTaskId());
                if (result != null && result.getSummary() != null) {
                    summaries.add(result.getSummary());
                    sections.add(ReportSection.builder()
                            .title(task.getDescription())
                            .content(result.getSummary())
                            .attributionAngle(task.getAttributionAngle())
                            .sourceTaskIds(List.of(task.getTaskId()))
                            .build());
                }
            }
        }

        return AttributionReport.builder()
                .title("归因分析报告")
                .executiveSummary(String.join("\n\n", summaries))
                .sections(sections)
                .keyFindings(summaries.stream().limit(3).toList())
                .build();
    }

    private AttributionReport buildEmptyReport(AgentContext context) {
        return AttributionReport.builder()
                .title("归因分析报告")
                .executiveSummary("无法生成归因分析报告，子任务规划为空。")
                .build();
    }
}