package com.datacopilotx.harness.agent.graph;

import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.common.constant.PromptConstant;
import com.datacopilotx.common.util.WorkflowUtil;
import com.datacopilotx.harness.agent.context.AgentContext;
import com.datacopilotx.harness.agent.domain.ExecutionResult;
import com.datacopilotx.harness.agent.domain.PredictionResult;
import com.datacopilotx.harness.agent.domain.SubTask;
import com.datacopilotx.harness.agent.domain.TaskDAG;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Predictor（预测智能体）—— 基于子任务执行数据与归因分析结论进行趋势预测
 * <p>
 * 核心职责：
 * <ol>
 *   <li>收集所有子任务的执行结果数据（rows/aggregations）</li>
 *   <li>结合归因报告摘要，调用 LLM 预测核心指标未来走势</li>
 *   <li>输出趋势数据点、指标预测、置信水平与风险提示</li>
 * </ol>
 * <p>
 * 解析失败时返回失败标记的空结果，不阻断报告生成。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PredictorAgent {

    private final AIGatewayChatService aiGatewayChatService;
    private final ObjectMapper objectMapper;

    /**
     * 基于执行数据与归因结论生成预测
     *
     * @param context 共享上下文（包含所有子任务执行结果）
     * @return 预测结果（解析失败时 success=false）
     */
    public PredictionResult predict(AgentContext context) {
        log.info("[Predictor] 开始基于 {} 个子任务结果进行数据预测", context.getExecutionResults().size());

        String userPrompt = buildPrompt(context);
        ChatRequest chatRequest = ChatRequest.builder()
                .systemPrompt(PromptConstant.HARNESS_PREDICTOR_SYSTEM_PROMPT)
                .userPrompt(userPrompt)
                .question(context.getOriginalQuestion())
                .model(context.getModel())
                .type(context.getModelType())
                .apiKey(context.getApiKey())
                .baseUrl(context.getBaseUrl())
                .platform(context.getPlatform())
                .build();

        String rawResponse = aiGatewayChatService.chatCompletions(chatRequest);
        log.debug("[Predictor] LLM 原始响应: {}", rawResponse);

        PredictionResult result = parseResponse(rawResponse);
        if (result == null) {
            log.warn("[Predictor] 预测结果解析失败，返回降级空结果");
            return PredictionResult.builder()
                    .success(false)
                    .trendPoints(new ArrayList<>())
                    .metrics(new ArrayList<>())
                    .risks(new ArrayList<>())
                    .build();
        }

        log.info("[Predictor] 预测完成，共 {} 个趋势点、{} 个指标",
                result.getTrendPoints().size(), result.getMetrics().size());
        return result;
    }

    /**
     * 构建预测提示词：原始问题 + 归因摘要 + 子任务执行数据
     */
    private String buildPrompt(AgentContext context) {
        // 归因摘要：执行摘要 + 关键发现
        String attributionSummary = "（归因报告尚未生成）";
        // 归因内容由调用方写入 context，此处从执行结果中提取摘要作为兜底
        TaskDAG dag = context.getTaskDAG();
        if (dag != null && !context.getExecutionResults().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (SubTask task : dag.getSubTasks()) {
                ExecutionResult result = context.getExecutionResult(task.getTaskId());
                if (result != null && result.getSummary() != null) {
                    sb.append("- ").append(task.getDescription()).append(": ").append(result.getSummary()).append("\n");
                }
            }
            if (!sb.isEmpty()) {
                attributionSummary = sb.toString();
            }
        }

        return String.format(PromptConstant.HARNESS_PREDICTOR_USER_PROMPT,
                context.getOriginalQuestion(),
                attributionSummary,
                buildExecutionData(context));
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
            if (result == null || !result.isSuccess()) {
                continue;
            }
            sb.append("### ").append(task.getDescription()).append("\n");
            if (result.getData() != null) {
                sb.append("- 数据: ").append(objectMapper.valueToTree(result.getData()).toString()).append("\n");
            }
            if (result.getSummary() != null) {
                sb.append("- 摘要: ").append(result.getSummary()).append("\n");
            }
            sb.append("\n");
        }
        return sb.isEmpty() ? "（无执行数据）" : sb.toString();
    }

    /**
     * 解析 LLM 返回的预测 JSON
     */
    private PredictionResult parseResponse(String rawResponse) {
        try {
            String cleaned = WorkflowUtil.cleanJsonStr(rawResponse);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(cleaned, Map.class);

            List<PredictionResult.TrendPoint> trendPoints = parseTrendPoints(map.get("trendPoints"));
            List<PredictionResult.ForecastMetric> metrics = parseMetrics(map.get("metrics"));

            return PredictionResult.builder()
                    .trendPoints(trendPoints)
                    .metrics(metrics)
                    .forecastSummary(asString(map.get("forecastSummary")))
                    .confidenceLevel(asString(map.get("confidenceLevel")))
                    .risks(asStringList(map.get("risks")))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.warn("[Predictor] 预测解析失败: {}", e.getMessage());
            return null;
        }
    }

    private List<PredictionResult.TrendPoint> parseTrendPoints(Object obj) {
        List<PredictionResult.TrendPoint> points = new ArrayList<>();
        if (!(obj instanceof List<?> list)) {
            return points;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                points.add(PredictionResult.TrendPoint.builder()
                        .label(asString(map.get("label")))
                        .value(asDouble(map.get("value")))
                        .isForecast(Boolean.TRUE.equals(map.get("isForecast")))
                        .build());
            }
        }
        return points;
    }

    private List<PredictionResult.ForecastMetric> parseMetrics(Object obj) {
        List<PredictionResult.ForecastMetric> metrics = new ArrayList<>();
        if (!(obj instanceof List<?> list)) {
            return metrics;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                metrics.add(PredictionResult.ForecastMetric.builder()
                        .name(asString(map.get("name")))
                        .currentValue(asDouble(map.get("currentValue")))
                        .forecastValue(asDouble(map.get("forecastValue")))
                        .changeRate(asDouble(map.get("changeRate")))
                        .build());
            }
        }
        return metrics;
    }

    private List<String> asStringList(Object obj) {
        if (obj instanceof List<?> list) {
            return list.stream().map(Object::toString).collect(Collectors.toList());
        }
        return new ArrayList<>();
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
