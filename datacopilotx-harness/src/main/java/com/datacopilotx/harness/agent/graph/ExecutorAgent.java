package com.datacopilotx.harness.agent.graph;

import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.common.constant.PromptConstant;
import com.datacopilotx.harness.agent.context.AgentContext;
import com.datacopilotx.harness.agent.domain.ExecutionResult;
import com.datacopilotx.harness.agent.domain.SubTask;
import com.datacopilotx.harness.agent.domain.TaskDAG;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Executor（执行智能体）—— 按依赖顺序执行 DAG 中的每个子任务
 * <p>
 * 核心职责：
 * <ol>
 *   <li>接收 Planner 生成的 {@link TaskDAG}</li>
 *   <li>按拓扑序遍历子任务，对于无依赖关系的同级子任务可并行执行</li>
 *   <li>将上游子任务的结果注入到下游子任务的执行上下文中</li>
 *   <li>每个子任务完成后，将结果写入 {@link AgentContext}</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutorAgent {

    private final AIGatewayChatService aiGatewayChatService;

    /** 并行执行器 —— 用于同层级无依赖子任务的并行执行 */
    private static final ExecutorService PARALLEL_EXECUTOR = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

    /**
     * 执行 DAG 中的所有子任务
     *
     * @param context          共享上下文（包含 DAG 和上游结果）
     * @param progressCallback 进度回调，用于 SSE 推送子任务执行进度
     */
    public void execute(AgentContext context, Consumer<String> progressCallback) {
        TaskDAG dag = context.getTaskDAG();
        if (dag == null || dag.getSubTasks().isEmpty()) {
            log.warn("[Executor] DAG 为空，跳过执行");
            return;
        }

        List<SubTask> sortedTasks = dag.getSubTasks();
        int totalTasks = sortedTasks.size();
        Set<String> completedTasks = ConcurrentHashMap.newKeySet();
        AtomicInteger completedCount = new AtomicInteger(0);

        log.info("[Executor] 开始执行 {} 个子任务", totalTasks);

        while (completedTasks.size() < sortedTasks.size()) {
            List<SubTask> readyTasks = sortedTasks.stream()
                    .filter(task -> !completedTasks.contains(task.getTaskId()))
                    .filter(task -> completedTasks.containsAll(task.getDependsOn()))
                    .toList();

            if (readyTasks.isEmpty() && completedTasks.size() < sortedTasks.size()) {
                log.error("[Executor] 存在未满足的依赖，但无可执行任务，可能存在死锁");
                break;
            }

            List<CompletableFuture<Void>> futures = readyTasks.stream()
                    .map(task -> CompletableFuture.runAsync(() -> {
                        int currentNum = completedCount.get() + 1;
                        String typeLabel = getTypeLabel(task.getType());
                        progressCallback.accept(String.format(
                                "正在执行子任务 %d/%d [%s]: %s",
                                currentNum, totalTasks, typeLabel, task.getDescription()
                        ));
                        log.info("[Executor] 开始执行子任务: {}", task.getTaskId());
                        long start = System.currentTimeMillis();

                        ExecutionResult result = executeSubTask(task, context);
                        result.setExecutionTimeMs(System.currentTimeMillis() - start);

                        context.putExecutionResult(result);
                        completedTasks.add(task.getTaskId());
                        int done = completedCount.incrementAndGet();

                        String summary = result.getSummary() != null
                                ? result.getSummary()
                                : (result.isSuccess() ? "执行成功" : "执行失败");
                        progressCallback.accept(String.format(
                                "子任务 %d/%d 完成: %s（耗时 %dms）—— %s",
                                done, totalTasks, task.getDescription(),
                                result.getExecutionTimeMs(), summary
                        ));
                        log.info("[Executor] 子任务 {} 完成，耗时 {}ms, 成功: {}",
                                task.getTaskId(), result.getExecutionTimeMs(), result.isSuccess());
                    }, PARALLEL_EXECUTOR))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        log.info("[Executor] 全部子任务执行完成，共 {} 个", completedTasks.size());
    }

    private String getTypeLabel(com.datacopilotx.common.constant.SubTaskType type) {
        if (type == null) return "UNKNOWN";
        return switch (type) {
            case NL2SQL -> "NL2SQL";
            case COMPARISON -> "对比分析";
            case AGGREGATION -> "聚合分析";
            case ML_PIPELINE -> "ML分析";
        };
    }

    /**
     * 执行单个子任务
     */
    private ExecutionResult executeSubTask(SubTask task, AgentContext context) {
        try {
            return switch (task.getType()) {
                case NL2SQL -> executeNL2SQL(task, context);
                case COMPARISON -> executeComparison(task, context);
                case AGGREGATION -> executeAggregation(task, context);
                case ML_PIPELINE -> executeMLPipeline(task, context);
            };
        } catch (Exception e) {
            log.error("[Executor] 子任务 {} 执行失败", task.getTaskId(), e);
            return ExecutionResult.failure(task.getTaskId(), e.getMessage());
        }
    }

    /**
     * 构建 ChatRequest —— 从 AgentContext 携带模型配置，确保 AIGatewayChatService 能正确路由
     */
    private ChatRequest buildChatRequest(String systemPrompt, String userPrompt, String question, AgentContext context) {
        return ChatRequest.builder()
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .question(question)
                .model(context.getModel())
                .type(context.getModelType())
                .apiKey(context.getApiKey())
                .baseUrl(context.getBaseUrl())
                .platform(context.getPlatform())
                .build();
    }

    /**
     * 执行 NL2SQL 类型子任务
     * <p>
     * 调用 LLM 生成 SQL 并模拟执行，或接入现有的 NL2SQL 能力
     */
    private ExecutionResult executeNL2SQL(SubTask task, AgentContext context) {
        String upstreamContext = buildUpstreamContext(task, context);
        String metricsStr = task.getMetrics() != null ? task.getMetrics().toString() : "";

        String userPrompt = String.format(PromptConstant.HARNESS_NL2SQL_USER_PROMPT,
                task.getDescription(), metricsStr, upstreamContext);

        ChatRequest chatRequest = buildChatRequest(
                PromptConstant.HARNESS_NL2SQL_SYSTEM_PROMPT,
                userPrompt, task.getDescription(), context);

        String rawResponse = aiGatewayChatService.chatCompletions(chatRequest);
        Map<String, Object> parsed = parseLLMJsonResponse(rawResponse);

        return ExecutionResult.builder()
                .taskId(task.getTaskId())
                .success(true)
                .data(parsed)
                .generatedSql((String) parsed.getOrDefault("sql", ""))
                .summary((String) parsed.getOrDefault("summary", task.getDescription()))
                .tokenUsage(chatRequest.getTokenUsage() != null ? chatRequest.getTokenUsage() : 0)
                .build();
    }

    /**
     * 执行对比分析类型子任务
     */
    private ExecutionResult executeComparison(SubTask task, AgentContext context) {
        String upstreamContext = buildUpstreamContext(task, context);

        String userPrompt = String.format(PromptConstant.HARNESS_COMPARISON_USER_PROMPT,
                task.getDescription(), upstreamContext);

        ChatRequest chatRequest = buildChatRequest(
                PromptConstant.HARNESS_COMPARISON_SYSTEM_PROMPT,
                userPrompt, task.getDescription(), context);

        String rawResponse = aiGatewayChatService.chatCompletions(chatRequest);
        Map<String, Object> parsed = parseLLMJsonResponse(rawResponse);

        return ExecutionResult.builder()
                .taskId(task.getTaskId())
                .success(true)
                .data(parsed)
                .summary((String) parsed.getOrDefault("summary", task.getDescription()))
                .tokenUsage(chatRequest.getTokenUsage() != null ? chatRequest.getTokenUsage() : 0)
                .build();
    }

    /**
     * 执行聚合计算类型子任务
     */
    private ExecutionResult executeAggregation(SubTask task, AgentContext context) {
        String upstreamContext = buildUpstreamContext(task, context);

        String userPrompt = String.format(PromptConstant.HARNESS_AGGREGATION_USER_PROMPT,
                task.getDescription(), upstreamContext);

        ChatRequest chatRequest = buildChatRequest(
                PromptConstant.HARNESS_AGGREGATION_SYSTEM_PROMPT,
                userPrompt, task.getDescription(), context);

        String rawResponse = aiGatewayChatService.chatCompletions(chatRequest);
        Map<String, Object> parsed = parseLLMJsonResponse(rawResponse);

        return ExecutionResult.builder()
                .taskId(task.getTaskId())
                .success(true)
                .data(parsed)
                .summary((String) parsed.getOrDefault("summary", task.getDescription()))
                .tokenUsage(chatRequest.getTokenUsage() != null ? chatRequest.getTokenUsage() : 0)
                .build();
    }

    /**
     * 执行 ML 流水线类型子任务
     * <p>
     * 预留扩展点 —— 可接入现有的 ML 流水线
     */
    private ExecutionResult executeMLPipeline(SubTask task, AgentContext context) {
        String upstreamContext = buildUpstreamContext(task, context);

        String userPrompt = String.format(PromptConstant.HARNESS_ML_PIPELINE_USER_PROMPT,
                task.getDescription(), upstreamContext);

        ChatRequest chatRequest = buildChatRequest(
                PromptConstant.HARNESS_ML_PIPELINE_SYSTEM_PROMPT,
                userPrompt, task.getDescription(), context);

        String rawResponse = aiGatewayChatService.chatCompletions(chatRequest);
        Map<String, Object> parsed = parseLLMJsonResponse(rawResponse);

        return ExecutionResult.builder()
                .taskId(task.getTaskId())
                .success(true)
                .data(parsed)
                .summary((String) parsed.getOrDefault("summary", task.getDescription()))
                .tokenUsage(chatRequest.getTokenUsage() != null ? chatRequest.getTokenUsage() : 0)
                .build();
    }

    /**
     * 构建上游子任务上下文 —— 将上游结果序列化为文本，注入下游子任务的 prompt
     */
    private String buildUpstreamContext(SubTask task, AgentContext context) {
        if (task.getDependsOn().isEmpty()) {
            return "无上游依赖";
        }

        Map<String, ExecutionResult> upstream = context.getUpstreamResults(task);
        if (upstream.isEmpty()) {
            return "无上游依赖";
        }

        return upstream.entrySet().stream()
                .map(entry -> String.format("### %s\n%s\n",
                        entry.getKey(),
                        entry.getValue().getSummary() != null
                                ? entry.getValue().getSummary()
                                : entry.getValue().getData()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 解析 LLM 返回的 JSON 响应
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseLLMJsonResponse(String rawResponse) {
        try {
            String cleaned = com.datacopilotx.common.util.WorkflowUtil.cleanJsonStr(rawResponse);
            if (cleaned.startsWith("{")) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(cleaned, Map.class);
            }
        } catch (Exception e) {
            log.warn("[Executor] JSON 解析失败: {}", e.getMessage());
        }
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("summary", rawResponse);
        fallback.put("raw", rawResponse);
        return fallback;
    }
}