package com.datacopilotx.harness.agent.orchestrator;

import com.datacopilotx.harness.agent.context.AgentContext;
import com.datacopilotx.harness.agent.domain.AttributionReport;
import com.datacopilotx.harness.agent.domain.TaskDAG;
import com.datacopilotx.harness.agent.executor.ExecutorAgent;
import com.datacopilotx.harness.agent.planner.PlannerAgent;
import com.datacopilotx.harness.agent.synthesizer.SynthesizerAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * 本地 Agent 编排器 —— Planner → Executor → Synthesizer 直连，无需 Redis
 * <p>
 * 职责：
 * <ol>
 *   <li>调用 PlannerAgent 将问题分解为子任务 DAG</li>
 *   <li>调用 ExecutorAgent 按依赖顺序执行子任务</li>
 *   <li>调用 SynthesizerAgent 综合所有结果生成归因分析报告</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentOrchestrator {

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

        // Phase 1: Planner
        log.info("[AgentOrchestrator] Phase 1: Planner 规划");
        progressCallback.accept("正在规划子任务...");
        TaskDAG dag = plannerAgent.plan(context);
        if (dag == null || dag.getSubTasks().isEmpty()) {
            return buildEmptyReport(context, "Planner 未能生成子任务");
        }
        context.setTaskDAG(dag);
        log.info("[AgentOrchestrator] Planner 完成，共 {} 个子任务", dag.getSubTasks().size());

        // Phase 2: Executor
        log.info("[AgentOrchestrator] Phase 2: Executor 执行");
        progressCallback.accept("正在执行子任务（共 " + dag.getSubTasks().size() + " 个）...");
        executorAgent.execute(context);
        log.info("[AgentOrchestrator] Executor 完成");

        // Phase 3: Synthesizer
        log.info("[AgentOrchestrator] Phase 3: Synthesizer 综合");
        progressCallback.accept("正在生成归因分析报告...");
        AttributionReport report = synthesizerAgent.synthesize(context);

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("[AgentOrchestrator] ===== 归因分析完成: session={}, 耗时 {}ms =====", sessionId, totalTime);

        return report;
    }

    private AttributionReport buildEmptyReport(AgentContext context, String reason) {
        return AttributionReport.builder()
                .title("归因分析报告")
                .executiveSummary(reason)
                .build();
    }
}