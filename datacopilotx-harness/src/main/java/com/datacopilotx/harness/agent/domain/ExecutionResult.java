package com.datacopilotx.harness.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 单个子任务的执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {

    /** 对应的子任务 ID */
    private String taskId;

    /** 执行是否成功 */
    @Builder.Default
    private boolean success = true;

    /** 错误信息（执行失败时） */
    private String errorMessage;

    /**
     * 结构化结果数据
     * 例如：{"rows": [...], "aggregations": {"total_sales": 100000}}
     */
    private Map<String, Object> data;

    /**
     * 自然语言摘要 —— 执行完成后由 LLM 生成的简要说明
     */
    private String summary;

    /**
     * 生成的 SQL（如果是 NL2SQL 类型）
     */
    private String generatedSql;

    /** 执行耗时（毫秒） */
    private long executionTimeMs;

    /** Token 消耗 */
    private int tokenUsage;

    public static ExecutionResult success(String taskId, Map<String, Object> data, String summary) {
        return ExecutionResult.builder()
                .taskId(taskId)
                .success(true)
                .data(data)
                .summary(summary)
                .build();
    }

    public static ExecutionResult failure(String taskId, String errorMessage) {
        return ExecutionResult.builder()
                .taskId(taskId)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}