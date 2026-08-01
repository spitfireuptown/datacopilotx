package com.datacopilotx.harness.agent.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 子任务有向无环图（DAG）
 * <p>
 * Planner 将复杂问题分解为多个子任务后，构建此 DAG 来描述子任务之间的依赖关系。
 * 支持拓扑排序，供 Executor 按依赖顺序执行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDAG {

    /** 原始复杂问题 */
    private String originalQuestion;

    /** 按拓扑序排列的子任务列表（由 Planner 生成时已排序） */
    @Builder.Default
    private List<SubTask> subTasks = new ArrayList<>();

    /**
     * 对子任务进行拓扑排序（Kahn 算法）
     * 如果子任务之间的依赖关系未排序，则调用此方法按拓扑序重排
     *
     * @return 拓扑排序后的子任务列表
     * @throws IllegalStateException 如果 DAG 中存在环
     */
    public List<SubTask> topologicalSort() {
        Map<String, SubTask> taskMap = new LinkedHashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacency = new HashMap<>();

        for (SubTask task : subTasks) {
            taskMap.put(task.getTaskId(), task);
            inDegree.putIfAbsent(task.getTaskId(), 0);
            adjacency.putIfAbsent(task.getTaskId(), new ArrayList<>());
        }

        for (SubTask task : subTasks) {
            for (String depId : task.getDependsOn()) {
                adjacency.computeIfAbsent(depId, k -> new ArrayList<>()).add(task.getTaskId());
                inDegree.merge(task.getTaskId(), 1, Integer::sum);
            }
        }

        List<SubTask> sorted = new ArrayList<>();
        List<String> queue = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        // 按优先级排序同级节点
        queue.sort((a, b) -> {
            int pa = taskMap.get(a).getPriority();
            int pb = taskMap.get(b).getPriority();
            return Integer.compare(pa, pb);
        });

        while (!queue.isEmpty()) {
            String current = queue.remove(0);
            sorted.add(taskMap.get(current));

            for (String neighbor : adjacency.getOrDefault(current, List.of())) {
                int newDegree = inDegree.merge(neighbor, -1, Integer::sum);
                if (newDegree == 0) {
                    queue.add(neighbor);
                }
            }
            queue.sort((a, b) -> {
                int pa = taskMap.get(a).getPriority();
                int pb = taskMap.get(b).getPriority();
                return Integer.compare(pa, pb);
            });
        }

        if (sorted.size() != subTasks.size()) {
            throw new IllegalStateException("DAG 中存在环，无法进行拓扑排序");
        }

        this.subTasks = sorted;
        return sorted;
    }

    /**
     * 获取指定子任务的所有上游任务 ID
     */
    public Set<String> getUpstreamTaskIds(String taskId) {
        Set<String> upstream = new HashSet<>();
        for (SubTask task : subTasks) {
            if (task.getTaskId().equals(taskId)) {
                collectUpstream(task, upstream);
                break;
            }
        }
        return upstream;
    }

    private void collectUpstream(SubTask task, Set<String> upstream) {
        for (String depId : task.getDependsOn()) {
            if (upstream.add(depId)) {
                subTasks.stream()
                        .filter(t -> t.getTaskId().equals(depId))
                        .findFirst()
                        .ifPresent(parent -> collectUpstream(parent, upstream));
            }
        }
    }

    /**
     * 获取无依赖的根任务列表
     */
    @JsonIgnore
    public List<SubTask> getRootTasks() {
        return subTasks.stream()
                .filter(t -> t.getDependsOn().isEmpty())
                .toList();
    }

    /**
     * 获取叶子任务列表（无下游依赖）
     */
    @JsonIgnore
    public List<SubTask> getLeafTasks() {
        Set<String> hasDependents = new HashSet<>();
        for (SubTask task : subTasks) {
            hasDependents.addAll(task.getDependsOn());
        }
        return subTasks.stream()
                .filter(t -> !hasDependents.contains(t.getTaskId()))
                .toList();
    }
}