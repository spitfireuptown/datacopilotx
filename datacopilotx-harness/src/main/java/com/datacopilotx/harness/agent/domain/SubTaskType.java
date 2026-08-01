package com.datacopilotx.harness.agent.domain;

/**
 * 子任务类型枚举
 */
public enum SubTaskType {

    /**
     * NL2SQL —— 将自然语言转为 SQL 查询数据库
     */
    NL2SQL,

    /**
     * ML 流水线 —— 调用机器学习流水线进行分析（如异常检测、趋势预测）
     */
    ML_PIPELINE,

    /**
     * 聚合计算 —— 对上游结果进行聚合、汇总
     */
    AGGREGATION,

    /**
     * 对比分析 —— 对不同维度/时间段的结果进行对比
     */
    COMPARISON
}
