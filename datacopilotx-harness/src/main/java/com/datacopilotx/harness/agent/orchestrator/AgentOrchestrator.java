package com.datacopilotx.harness.agent.orchestrator;

import com.datacopilotx.harness.agent.context.AgentContext;
import com.datacopilotx.harness.agent.domain.AttributionReport;
import com.datacopilotx.harness.agent.domain.TaskDAG;
import com.datacopilotx.harness.agent.graph.ExecutorAgent;
import com.datacopilotx.harness.agent.graph.PlannerAgent;
import com.datacopilotx.harness.agent.graph.ScopeAnalyzerAgent;
import com.datacopilotx.harness.agent.graph.SynthesizerAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * 本地 Agent 编排器 —— ScopeAnalyzer → Planner → Executor → Synthesizer 直连，无需 Redis
 * <p>
 * 职责：
 * <ol>
 *   <li>调用 ScopeAnalyzer 分析问题涉及的数据表，收拢数据边界</li>
 *   <li>调用 PlannerAgent 将问题分解为子任务 DAG</li>
 *   <li>调用 ExecutorAgent 按依赖顺序执行子任务</li>
 *   <li>调用 SynthesizerAgent 综合所有结果生成归因分析报告</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final ScopeAnalyzerAgent scopeAnalyzer;
    private final PlannerAgent plannerAgent;
    private final ExecutorAgent executorAgent;
    private final SynthesizerAgent synthesizerAgent;

    /**
     * 执行归因分析全流程
     *
     * @param context          分析上下文（携带模型配置）
     * @param progressCallback 进度回调，用于 SSE 推送中间状态
     * @return 归因分析报告
     */
    public AttributionReport analyze(AgentContext context, Consumer<String> progressCallback) {
        String sessionId = context.getSessionId();
        long startTime = System.currentTimeMillis();
        log.info("[AgentOrchestrator] ===== 归因分析开始: session={} =====", sessionId);

        // Phase 0: Scope Analysis —— 分析问题涉及哪些数据表，收拢数据边界
        progressCallback.accept("Step 1/4: 正在分析问题涉及的数据表，收拢数据边界...");
        log.info("[AgentOrchestrator] Phase 0: Scope 分析");
        String narrowedScope = scopeAnalyzer.analyze(context);
        context.setNarrowedScope(narrowedScope);
        log.info("[AgentOrchestrator] Scope 分析完成");
        progressCallback.accept("Step 1/4 完成: 数据边界已收拢");

        // Phase 1: Planner
        progressCallback.accept("Step 2/4: 正在规划子任务...");
        log.info("[AgentOrchestrator] Phase 1: Planner 规划");
        TaskDAG dag = plannerAgent.plan(context);
        if (dag == null || dag.getSubTasks().isEmpty()) {
            progressCallback.accept("规划失败：未能生成子任务");
            return buildEmptyReport(context, "Planner 未能生成子任务");
        }
        context.setTaskDAG(dag);
        log.info("[AgentOrchestrator] Planner 完成，共 {} 个子任务", dag.getSubTasks().size());
        progressCallback.accept(String.format("Step 2/4 完成: 已规划 %d 个子任务", dag.getSubTasks().size()));

        // Phase 2: Executor
        progressCallback.accept(String.format("Step 3/4: 正在执行子任务（共 %d 个）...", dag.getSubTasks().size()));
        log.info("[AgentOrchestrator] Phase 2: Executor 执行");
        executorAgent.execute(context, progressCallback);
        log.info("[AgentOrchestrator] Executor 完成");

        // Phase 3: Synthesizer
        progressCallback.accept("Step 4/4: 正在综合子任务结果，生成归因分析报告...");
        log.info("[AgentOrchestrator] Phase 3: Synthesizer 综合");
        AttributionReport report = synthesizerAgent.synthesize(context);

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("[AgentOrchestrator] ===== 归因分析完成: session={}, 耗时 {}ms =====", sessionId, totalTime);
        progressCallback.accept(String.format("归因分析完成，总耗时 %dms", totalTime));

        return report;
    }

    private AttributionReport buildEmptyReport(AgentContext context, String reason) {
        return AttributionReport.builder()
                .title("归因分析报告")
                .executiveSummary(reason)
                .build();
    }
}