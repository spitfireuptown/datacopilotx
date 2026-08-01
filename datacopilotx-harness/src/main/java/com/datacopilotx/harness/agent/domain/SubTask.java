package com.datacopilotx.harness.agent.domain;

import com.datacopilotx.common.constant.SubTaskType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 子任务定义 —— Planner 将复杂问题分解为 DAG 中的节点
 * <p>
 * 每个子任务描述一个独立的分析步骤，其依赖关系由 {@link TaskDAG} 管理。
 * 执行引擎（Executor）会按拓扑序执行每个子任务，并将上游结果注入到当前子任务的上下文中。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubTask {

    /** 子任务唯一标识（由 Planner 分配） */
    private String taskId;

    /** 子任务描述 —— 自然语言描述该子任务要回答的问题 */
    private String description;

    /** 子任务类型：NL2SQL / ML_PIPELINE / AGGREGATION / COMPARISON */
    private SubTaskType type;

    /**
     * 数据源信息 —— 该子任务需要查询的数据集/表名
     * 对于 NL2SQL 类型，这里指明需要分析的数据表
     */
    private String dataSource;

    /**
     * 维度/指标描述 —— 该子任务关注的维度与指标
     * 例如：{"dimensions": ["地区", "时间"], "metrics": ["销售额", "利润"]}
     */
    private Map<String, Object> metrics;

    /**
     * 依赖的子任务 ID 列表 —— 当前子任务需要等待哪些上游子任务完成后才能执行
     */
    @Builder.Default
    private List<String> dependsOn = new ArrayList<>();

    /**
     * 子任务优先级 —— 在同层级中，数值越小越优先执行
     */
    @Builder.Default
    private int priority = 0;

    /**
     * 该子任务是否为核心归因子任务
     * 核心归因子任务的结果会被 Synthesizer 重点引用
     */
    @Builder.Default
    private boolean attributionCore = false;

    /**
     * 归因分析角度 —— 如 "drill_down"（下钻）、"comparison"（对比）、"contribution"（贡献度）
     */
    private String attributionAngle;
}