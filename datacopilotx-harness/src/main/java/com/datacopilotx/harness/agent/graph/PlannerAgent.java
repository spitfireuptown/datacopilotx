package com.datacopilotx.harness.agent.graph;

import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.common.constant.PromptConstant;
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
     * 规划 —— 将原始问题分解为子任务 DAG
     *
     * @param context 上下文（包含原始问题、数据源信息等）
     * @return 包含子任务列表的 TaskDAG
     */
    public TaskDAG plan(AgentContext context) {
        log.info("[Planner] 开始规划，原始问题: {}", context.getOriginalQuestion());

        String userPrompt = buildPlanningPrompt(context);
        ChatRequest chatRequest = ChatRequest.builder()
                .systemPrompt(PromptConstant.HARNESS_PLANNER_SYSTEM_PROMPT)
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
        String narrowedScope = context.getNarrowedScope();
        String dataSourceSection = "";

        if (narrowedScope != null && !narrowedScope.isBlank()) {
            dataSourceSection = String.format("""
                    ## 可用数据表（已收拢，仅包含与问题相关的表）
                    %s
                    """, narrowedScope);
        } else if (context.getDataSourceInfo() != null && !context.getDataSourceInfo().isBlank()) {
            dataSourceSection = String.format("""
                    ## 可用数据表
                    %s
                    """, context.getDataSourceInfo());
        }

        return String.format(PromptConstant.HARNESS_PLANNER_USER_PROMPT,
                context.getOriginalQuestion(), dataSourceSection);
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