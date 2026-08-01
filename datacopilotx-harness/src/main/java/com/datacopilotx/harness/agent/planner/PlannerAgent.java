package com.datacopilotx.harness.agent.planner;

import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.common.util.WorkflowUtil;
import com.datacopilotx.harness.agent.context.AgentContext;
import com.datacopilotx.harness.agent.domain.SubTask;
import com.datacopilotx.harness.agent.domain.TaskDAG;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Planner（规划智能体）—— 负责将复杂问题分解为有依赖关系的子任务 DAG
 * <p>
 * 核心职责：
 * <ol>
 *   <li>接收用户的复杂归因分析问题</li>
 *   <li>调用 LLM 将问题分解为多个子任务</li>
 *   <li>识别子任务之间的依赖关系，构建 DAG</li>
 *   <li>输出 {@link TaskDAG} 供 Executor 执行</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlannerAgent {

    private final AIGatewayChatService aiGatewayChatService;
    private final ObjectMapper objectMapper;

    /**
     * Planner 规划提示词 —— 指导 LLM 将复杂问题分解为子任务 DAG
     */
    private static final String PLANNER_SYSTEM_PROMPT = """
            你是一个归因分析规划专家，擅长将复杂的"智能问数"归因分析问题分解为有依赖关系的子任务。

            ## 你的任务
            将用户问题分解为多个子任务，每个子任务是一个独立的分析步骤，子任务之间可能存在依赖关系（DAG）。

            ## 子任务类型
            - NL2SQL：需要查询数据库获取数据，指定 dataSource（表名）和 metrics（维度/指标）
            - ML_PIPELINE：需要调用机器学习流水线（如异常检测、趋势预测、贡献度分析）
            - AGGREGATION：对上游子任务的结果进行聚合计算
            - COMPARISON：对不同维度的结果进行对比分析

            ## 依赖关系规则
            - 如果子任务B需要子任务A的结果，则B依赖A（在 dependsOn 中填写A的taskId）
            - 叶子任务（无下游依赖）通常是最终归因结论的直接支撑
            - 将最重要的归因维度子任务标记为 attributionCore=true

            ## 输出格式
            必须返回严格的JSON数组，每个元素是一个子任务：
            ```json
            [
              {
                "taskId": "task_1",
                "description": "子任务描述",
                "type": "NL2SQL",
                "dataSource": "表名",
                "metrics": {"dimensions": ["维度1"], "measures": ["指标1"]},
                "dependsOn": [],
                "priority": 0,
                "attributionCore": true,
                "attributionAngle": "drill_down"
              }
            ]
            ```
            """;

    /**
     * 规划 —— 将原始问题分解为子任务 DAG
     *
     * @param context 上下文（包含原始问题、数据源信息等）
     * @return 包含子任务列表的 TaskDAG
     */
    public TaskDAG plan(AgentContext context) {
        log.info("[Planner] 开始规划，原始问题: {}", context.getOriginalQuestion());

        String userPrompt = buildPlanningPrompt(context);
        ChatRequest chatRequest = ChatRequest.builder()
                .systemPrompt(PLANNER_SYSTEM_PROMPT)
                .userPrompt(userPrompt)
                .question(context.getOriginalQuestion())
                .model(context.getModel())
                .type(context.getModelType())
                .apiKey(context.getApiKey())
                .baseUrl(context.getBaseUrl())
                .platform(context.getPlatform())
                .build();

        String rawResponse = aiGatewayChatService.chatCompletions(chatRequest);
        log.debug("[Planner] LLM 原始响应: {}", rawResponse);

        List<SubTask> subTasks = parseSubTasks(rawResponse);
        log.info("[Planner] 分解出 {} 个子任务", subTasks.size());

        TaskDAG dag = TaskDAG.builder()
                .originalQuestion(context.getOriginalQuestion())
                .subTasks(subTasks)
                .build();

        // 确保拓扑排序
        dag.topologicalSort();
        log.info("[Planner] DAG 拓扑排序完成，根任务: {}",
                dag.getRootTasks().stream().map(SubTask::getTaskId).collect(Collectors.joining(", ")));

        return dag;
    }

    /**
     * 构建规划提示词
     */
    private String buildPlanningPrompt(AgentContext context) {
        return String.format("""
                ## 用户原始问题
                %s

                ## 要求
                1. 将问题分解为 3-8 个子任务
                2. 优先分解为归因分析维度：下钻分析（哪个维度贡献最大）、对比分析（同比/环比变化）、异常检测（哪些指标异常）
                3. 明确子任务之间的依赖关系（dependsOn）
                4. NL2SQL 类型子任务需要指定 dataSource 和 metrics
                5. 至少有一个子任务标记为 attributionCore=true
                """, context.getOriginalQuestion());
    }

    /**
     * 解析 LLM 返回的 JSON 为子任务列表
     */
    private List<SubTask> parseSubTasks(String rawResponse) {
        try {
            String cleanedJson = WorkflowUtil.cleanJsonStr(rawResponse);
            // 如果 cleanJsonStr 返回了原始字符串（没有 json 代码块），直接尝试解析
            List<SubTask> tasks = objectMapper.readValue(cleanedJson, new TypeReference<List<SubTask>>() {});
            return tasks != null ? tasks : new ArrayList<>();
        } catch (JsonProcessingException e) {
            log.warn("[Planner] JSON 解析失败，尝试修复: {}", e.getMessage());
            try {
                // 兜底：尝试直接解析整个响应
                List<SubTask> tasks = objectMapper.readValue(rawResponse, new TypeReference<List<SubTask>>() {});
                return tasks != null ? tasks : new ArrayList<>();
            } catch (JsonProcessingException ex) {
                log.error("[Planner] 无法解析子任务列表", ex);
                return new ArrayList<>();
            }
        }
    }
}