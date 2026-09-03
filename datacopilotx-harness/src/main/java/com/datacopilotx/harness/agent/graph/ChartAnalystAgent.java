package com.datacopilotx.harness.agent.graph;

import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.common.constant.PromptConstant;
import com.datacopilotx.common.util.WorkflowUtil;
import com.datacopilotx.harness.agent.context.AgentContext;
import com.datacopilotx.harness.agent.domain.ChartSpec;
import com.datacopilotx.harness.agent.domain.ExecutionResult;
import com.datacopilotx.harness.agent.domain.SubTask;
import com.datacopilotx.harness.agent.domain.TaskDAG;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ChartAnalyst（图表智能体）—— 从子任务执行数据中提取可图表化数据并生成解释
 * <p>
 * 核心职责：
 * <ol>
 *   <li>收集所有子任务的执行结果数据</li>
 *   <li>调用 LLM 筛选适合可视化的数据并决定图表类型</li>
 *   <li>校验数据点合法性，生成图表规格与自然语言解释</li>
 * </ol>
 * <p>
 * 解析失败时返回空列表，不阻断报告生成。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChartAnalystAgent {

    /** 最多生成的图表数量 */
    private static final int MAX_CHARTS = 3;

    private final AIGatewayChatService aiGatewayChatService;
    private final ObjectMapper objectMapper;

    /**
     * 从执行数据中提取可图表化数据
     *
     * @param context 共享上下文（包含所有子任务执行结果）
     * @return 图表规格列表（可能为空）
     */
    public List<ChartSpec> analyze(AgentContext context) {
        log.info("[ChartAnalyst] 开始分析 {} 个子任务结果中的可图表化数据", context.getExecutionResults().size());

        String userPrompt = String.format(PromptConstant.HARNESS_CHART_ANALYST_USER_PROMPT,
                context.getOriginalQuestion(),
                buildExecutionData(context));

        ChatRequest chatRequest = ChatRequest.builder()
                .systemPrompt(PromptConstant.HARNESS_CHART_ANALYST_SYSTEM_PROMPT)
                .userPrompt(userPrompt)
                .question(context.getOriginalQuestion())
                .model(context.getModel())
                .type(context.getModelType())
                .apiKey(context.getApiKey())
                .baseUrl(context.getBaseUrl())
                .platform(context.getPlatform())
                .build();

        String rawResponse = aiGatewayChatService.chatCompletions(chatRequest);
        log.debug("[ChartAnalyst] LLM 原始响应: {}", rawResponse);

        List<ChartSpec> charts = parseResponse(rawResponse);
        log.info("[ChartAnalyst] 图表分析完成，共 {} 张图表", charts.size());
        return charts;
    }

    /**
     * 拼接所有子任务的执行数据
     */
    private String buildExecutionData(AgentContext context) {
        TaskDAG dag = context.getTaskDAG();
        if (dag == null) {
            return "（无执行数据）";
        }
        StringBuilder sb = new StringBuilder();
        for (SubTask task : dag.getSubTasks()) {
            ExecutionResult result = context.getExecutionResult(task.getTaskId());
            if (result == null || !result.isSuccess() || result.getData() == null) {
                continue;
            }
            sb.append("### ").append(task.getDescription()).append("\n");
            sb.append("- 数据: ").append(objectMapper.valueToTree(result.getData()).toString()).append("\n");
            if (result.getSummary() != null) {
                sb.append("- 摘要: ").append(result.getSummary()).append("\n");
            }
            sb.append("\n");
        }
        return sb.isEmpty() ? "（无执行数据）" : sb.toString();
    }

    /**
     * 解析 LLM 返回的图表 JSON，并校验每个图表的数据点合法性
     */
    private List<ChartSpec> parseResponse(String rawResponse) {
        try {
            String cleaned = WorkflowUtil.cleanJsonStr(rawResponse);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(cleaned, Map.class);

            Object chartsObj = map.get("charts");
            if (!(chartsObj instanceof List<?> list)) {
                return new ArrayList<>();
            }

            List<ChartSpec> charts = new ArrayList<>();
            for (Object item : list) {
                if (charts.size() >= MAX_CHARTS) {
                    break;
                }
                if (item instanceof Map<?, ?> chartMap) {
                    ChartSpec chart = parseChart(chartMap);
                    if (chart != null) {
                        charts.add(chart);
                    }
                }
            }
            return charts;
        } catch (Exception e) {
            log.warn("[ChartAnalyst] 图表解析失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 解析单个图表，数据点为空或图表类型非法时返回 null
     */
    private ChartSpec parseChart(Map<?, ?> chartMap) {
        String chartType = asString(chartMap.get("chartType"));
        if (ObjectUtils.isEmpty(chartType)
                || !List.of("line", "bar", "pie").contains(chartType)) {
            return null;
        }

        List<ChartSpec.ChartDataPoint> dataPoints = new ArrayList<>();
        if (chartMap.get("data") instanceof List<?> dataList) {
            for (Object point : dataList) {
                if (point instanceof Map<?, ?> pointMap) {
                    String label = asString(pointMap.get("label"));
                    if (ObjectUtils.isEmpty(label)) {
                        continue;
                    }
                    dataPoints.add(ChartSpec.ChartDataPoint.builder()
                            .label(label)
                            .value(asDouble(pointMap.get("value")))
                            .build());
                }
            }
        }

        if (dataPoints.isEmpty()) {
            return null;
        }

        return ChartSpec.builder()
                .title(asString(chartMap.get("title")))
                .chartType(chartType)
                .xField(asString(chartMap.get("xField")))
                .yField(asString(chartMap.get("yField")))
                .data(dataPoints)
                .explanation(asString(chartMap.get("explanation")))
                .build();
    }

    private String asString(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    private double asDouble(Object obj) {
        if (obj instanceof Number number) {
            return number.doubleValue();
        }
        if (obj != null) {
            try {
                return Double.parseDouble(obj.toString());
            } catch (NumberFormatException ignored) {
                // 无法解析时返回 0
            }
        }
        return 0.0;
    }
}
