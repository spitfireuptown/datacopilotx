package com.datacopilotx.harness.agent.scope;

import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.common.util.WorkflowUtil;
import com.datacopilotx.harness.agent.context.AgentContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * ScopeAnalyzer（作用域分析智能体）—— 在规划前先分析问题涉及哪些数据表，收拢数据边界
 * <p>
 * 核心职责：
 * <ol>
 *   <li>接收用户问题 + 数据集下全部表的 schema 信息</li>
 *   <li>调用 LLM 分析问题语义，识别真正相关的数据表</li>
 *   <li>输出收拢后的数据边界（只包含相关表及其字段），供 PlannerAgent 使用</li>
 * </ol>
 * <p>
 * 设计意图：避免 PlannerAgent 面对过多无关表时产生幻觉，提高子任务规划的准确性。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScopeAnalyzer {

    private final AIGatewayChatService aiGatewayChatService;
    private final ObjectMapper objectMapper;

    private static final String SCOPE_SYSTEM_PROMPT = """
            你是一个数据作用域分析专家，擅长根据用户问题判断需要用到哪些数据表。

            ## 你的任务
            根据用户问题，从数据集的全部表中筛选出与问题相关的表，并说明每张表的用途。

            ## 输入
            1. 用户问题
            2. 数据集下全部表的 schema 信息（表名、字段、描述）

            ## 输出格式
            必须返回严格的 JSON 对象，包含两个字段：
            ```json
            {
              "relevantTables": ["table1", "table2"],
              "narrowedSchema": "只包含相关表的 schema 描述文本",
              "reasoning": "简短说明为什么选择这些表"
            }
            ```

            ## 筛选规则
            - 仔细阅读问题，提取问题中涉及的业务实体、维度、指标
            - 将业务实体、维度、指标与表的字段名、字段描述进行匹配
            - 如果问题与某张表的字段存在语义关联，则该表是相关的
            - 如果无法确定某张表是否相关，保守起见保留该表
            - narrowedSchema 只需要包含筛选出的表的 schema 信息，格式与输入一致
            """;

    /**
     * 分析问题涉及的数据表，收拢数据边界
     *
     * @param context 上下文（包含原始问题和 dataSourceInfo）
     * @return 收拢后的 schema 文本（只包含相关表的信息）
     */
    public String analyze(AgentContext context) {
        String originalQuestion = context.getOriginalQuestion();
        String dataSourceInfo = context.getDataSourceInfo();

        if (dataSourceInfo == null || dataSourceInfo.isBlank()) {
            log.warn("[ScopeAnalyzer] dataSourceInfo 为空，跳过作用域分析");
            return dataSourceInfo;
        }

        log.info("[ScopeAnalyzer] 开始分析数据边界，问题: {}", originalQuestion);

        String userPrompt = String.format("""
                ## 用户问题
                %s

                ## 数据集全部表信息
                %s

                ## 要求
                1. 分析问题涉及哪些业务实体、维度、指标
                2. 从全部表中筛选出与问题相关的表
                3. narrowedSchema 中只保留相关表的完整 schema 信息
                4. 如果只有一张表或全部表都相关，直接返回全部表的 schema
                """, originalQuestion, dataSourceInfo);

        ChatRequest chatRequest = ChatRequest.builder()
                .systemPrompt(SCOPE_SYSTEM_PROMPT)
                .userPrompt(userPrompt)
                .question(originalQuestion)
                .model(context.getModel())
                .type(context.getModelType())
                .apiKey(context.getApiKey())
                .baseUrl(context.getBaseUrl())
                .platform(context.getPlatform())
                .build();

        String rawResponse = aiGatewayChatService.chatCompletions(chatRequest);
        log.debug("[ScopeAnalyzer] LLM 原始响应: {}", rawResponse);

        String narrowedScope = parseScopeResponse(rawResponse, dataSourceInfo);
        log.info("[ScopeAnalyzer] 作用域分析完成，原始表数: {}, 收拢后相关表: {}",
                countTables(dataSourceInfo), countTables(narrowedScope));

        return narrowedScope;
    }

    /**
     * 解析 LLM 返回的作用域分析结果
     */
    private String parseScopeResponse(String rawResponse, String fallbackDataSourceInfo) {
        try {
            String cleanedJson = WorkflowUtil.cleanJsonStr(rawResponse);
            Map<String, Object> result = objectMapper.readValue(cleanedJson,
                    new TypeReference<Map<String, Object>>() {});
            if (result != null && result.containsKey("narrowedSchema")) {
                String narrowedSchema = (String) result.get("narrowedSchema");
                List<?> relevantTables = (List<?>) result.get("relevantTables");
                if (narrowedSchema != null && !narrowedSchema.isBlank()) {
                    log.info("[ScopeAnalyzer] 筛选出 {} 张相关表: {}", 
                            relevantTables != null ? relevantTables.size() : 0, relevantTables);
                    return narrowedSchema;
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("[ScopeAnalyzer] JSON 解析失败，使用全部 schema: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("[ScopeAnalyzer] 解析异常，使用全部 schema: {}", e.getMessage());
        }
        // 兜底：返回全部 schema
        return fallbackDataSourceInfo;
    }

    /**
     * 统计 schema 文本中的表数量（简单统计 "表名:" 出现次数）
     */
    private int countTables(String schemaInfo) {
        if (schemaInfo == null || schemaInfo.isBlank()) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while ((idx = schemaInfo.indexOf("**表名:", idx)) != -1) {
            count++;
            idx += 6;
        }
        // 也统计主表
        if (schemaInfo.contains("**主表名:")) {
            count++;
        }
        return count;
    }
}