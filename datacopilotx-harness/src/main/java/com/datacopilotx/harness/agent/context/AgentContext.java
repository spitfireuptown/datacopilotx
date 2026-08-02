package com.datacopilotx.harness.agent.context;

import com.datacopilotx.harness.agent.domain.ExecutionResult;
import com.datacopilotx.harness.agent.domain.SubTask;
import com.datacopilotx.harness.agent.domain.TaskDAG;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 共享上下文 —— 贯穿 Planner → Executor → Synthesizer 全流程
 * <p>
 * 保存原始问题、数据源信息、子任务 DAG、各子任务执行结果等。
 * 各智能体通过此上下文交换信息，Executor 将上游结果写入后供下游子任务读取。
 */
@Data
@Builder
public class AgentContext {

    /** 会话标识 */
    private String sessionId;

    /** 用户原始问题 */
    private String originalQuestion;

    /** 数据集 ID */
    private Long datasetId;

    /** 模型配置 ID */
    private Long modelId;

    /** 模型名称（如 gpt-4, qwen2.5） */
    private String model;

    /** 模型类型（openai / ollama），用于路由到对应的 AI 服务 */
    private String modelType;

    /** API Key */
    private String apiKey;

    /** API Base URL */
    private String baseUrl;

    /** 模型平台 */
    private String platform;

    /** 用户 ID */
    private String userId;

    /**
     * 数据集下所有表的 schema 信息（表名、字段、描述等）
     * 由 ChatService 在构建上下文时组装，供 ScopeAnalyzer 和 PlannerAgent 使用
     */
    private String dataSourceInfo;

    /**
     * 收拢后的数据边界 —— ScopeAnalyzer 从 dataSourceInfo 中筛选出的与问题相关的表信息
     * 只包含问题涉及的表及其 schema，PlannerAgent 基于此进行子任务规划
     */
    private String narrowedScope;

    /** Planner 生成的子任务 DAG */
    private TaskDAG taskDAG;

    /**
     * 子任务执行结果映射 —— key: taskId, value: ExecutionResult
     * 使用 ConcurrentHashMap 支持并发安全
     */
    @Builder.Default
    private Map<String, ExecutionResult> executionResults = new ConcurrentHashMap<>();

    /** 全局开始时间 */
    @Builder.Default
    private long startTimeMs = System.currentTimeMillis();

    /**
     * 获取指定子任务 ID 的执行结果
     */
    public ExecutionResult getExecutionResult(String taskId) {
        return executionResults.get(taskId);
    }

    /**
     * 存放子任务执行结果
     */
    public void putExecutionResult(ExecutionResult result) {
        executionResults.put(result.getTaskId(), result);
    }

    /**
     * 获取上游子任务的执行结果，用于传入下游子任务
     */
    public Map<String, ExecutionResult> getUpstreamResults(SubTask task) {
        Map<String, ExecutionResult> upstream = new ConcurrentHashMap<>();
        for (String depId : task.getDependsOn()) {
            ExecutionResult result = executionResults.get(depId);
            if (result != null) {
                upstream.put(depId, result);
            }
        }
        return upstream;
    }

    /**
     * 计算总耗时
     */
    public long getElapsedTimeMs() {
        return System.currentTimeMillis() - startTimeMs;
    }

    /**
     * 计算总 Token 消耗
     */
    public int getTotalTokenUsage() {
        return executionResults.values().stream()
                .mapToInt(ExecutionResult::getTokenUsage)
                .sum();
    }
}