package com.datacopilotx.ai.service.graph.main;

import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datacopilotx.ai.controller.form.QuestionForm;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.DataTableBean;
import com.datacopilotx.ai.domian.bean.DatasetRelationBean;
import com.datacopilotx.ai.domian.bean.ModelConfigBean;
import com.datacopilotx.ai.domian.dto.DataSetDTO;
import com.datacopilotx.ai.mapper.DataSetMapper;
import com.datacopilotx.ai.mapper.DataTableMapper;
import com.datacopilotx.ai.mapper.DatasetRelationMapper;
import com.datacopilotx.ai.mapper.ModelConfigMapper;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.common.result.WebResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class WorkflowServiceHelper {

    @Resource
    private DataSetMapper dataSetMapper;

    @Resource
    private DataTableMapper dataTableMapper;

    @Resource
    private DatasetRelationMapper datasetRelationMapper;

    @Resource
    private AIGatewayChatService aiGatewayChatService;

    @Resource
    private ModelConfigMapper modelConfigMapper;

    private static final int DEFAULT_TOP_K = 5;
    
    public Pair<String, String> injectPrompt(Pair<String, String> promptPair, Map<String, String> params) {
        String systemPrompt = promptPair.getKey();
        String userPrompt = promptPair.getValue();

        for (Map.Entry<String, String> stringStringEntry : params.entrySet()) {
            systemPrompt = systemPrompt.replace(stringStringEntry.getKey(), stringStringEntry.getValue());
            userPrompt = userPrompt.replace(stringStringEntry.getKey(), stringStringEntry.getValue());
        }
        return new Pair<>(systemPrompt, userPrompt);
    }


    public String assembleDataSetInfo(DataSetBean dataSetBean, String question, WorkflowState state) {
        if (state != null && state.getCachedDataSetInfo() != null) {
            log.debug("Using cached dataset info");
            return state.getCachedDataSetInfo();
        }

        String result = assembleDataSetInfo(dataSetBean, question);
        
        if (state != null) {
            state.setCachedDataSetInfo(result);
        }
        
        return result;
    }

    public String assembleDataSetInfo(DataSetBean dataSetBean, String question) {
        StringBuilder result = new StringBuilder();

        List<DataTableBean> allTables = dataTableMapper.selectList(new LambdaQueryWrapper<DataTableBean>()
                .eq(DataTableBean::getDatasetId, dataSetBean.getId())
                .eq(DataTableBean::getIsDel, 0));

        if (allTables.isEmpty()) {
            if ("excel".equalsIgnoreCase(dataSetBean.getType())) {
                result.append(String.format("**数据集名称:** %s\n", dataSetBean.getDsName()));
                result.append(String.format("**数据集描述:** %s\n", dataSetBean.getDescription() != null ? dataSetBean.getDescription() : ""));
            } else {
                result.append(String.format("**数据库:** %s\n", dataSetBean.getDatabase()));
                result.append(String.format("**数据集描述:** %s\n", dataSetBean.getDescription() != null ? dataSetBean.getDescription() : ""));
            }
            return result.toString();
        }

        List<DataTableBean> selectedTables = selectTablesByEmbedding(allTables, question);

        for (int i = 0; i < selectedTables.size(); i++) {
            DataTableBean tableBean = selectedTables.get(i);
            List<DataSetDTO.SchemaInfo> schemaInfos = JSONUtil.toList(tableBean.getFields(), DataSetDTO.SchemaInfo.class);
            String tableName = tableBean.getTable();

            if (i == 0) {
                result.append(String.format("**主表名:** %s\n", tableName));
                result.append(String.format("**主表描述:** %s\n", dataSetBean.getDescription() != null ? dataSetBean.getDescription() : ""));
                if (tableBean.getInjectPrompt() != null && !tableBean.getInjectPrompt().isEmpty()) {
                    result.append(String.format("**主表提示:** %s\n", tableBean.getInjectPrompt()));
                }
                result.append("**主表字段:**\n");
            } else {
                result.append(String.format("\n**表名:** %s\n", tableName));
                if (tableBean.getInjectPrompt() != null && !tableBean.getInjectPrompt().isEmpty()) {
                    result.append(String.format("**表提示:** %s\n", tableBean.getInjectPrompt()));
                }
                result.append("**表字段:**\n");
            }

            for (DataSetDTO.SchemaInfo field : schemaInfos) {
                result.append(String.format("    - 字段名：%s | 类型：%s | 描述：%s\n",
                        field.getFieldName(), field.getFieldType(),
                        field.getDescription() != null ? field.getDescription() : ""));
            }
        }

        List<RelationMeta> relationMetas = new ArrayList<>();

        List<DatasetRelationBean> dbRelations = datasetRelationMapper.selectByDatasetId(dataSetBean.getId());
        if (dbRelations != null && !dbRelations.isEmpty()) {
            for (DatasetRelationBean rel : dbRelations) {
                relationMetas.add(new RelationMeta(
                        null,
                        rel.getLeftTable(),
                        rel.getLeftField(),
                        rel.getRightTable(),
                        rel.getRightField(),
                        rel.getRelationType(),
                        true
                ));
            }
        }

        if (!relationMetas.isEmpty()) {
            result.append("\n**联表关系:**\n");
            for (RelationMeta rm : relationMetas) {
                String joinType = rm.getJoinType() != null ? rm.getJoinType() : "INNER JOIN";
                String fromTable = rm.getFromTable();
                String fromField = rm.getFromField();
                String toTable = rm.getToTable();
                String toField = rm.getToField();

                result.append(String.format("    %s %s ON %s.%s = %s.%s\n",
                        joinType, toTable, fromTable, fromField, toTable, toField));
            }
        }

        return result.toString();
    }

    private List<DataTableBean> selectTablesByEmbedding(List<DataTableBean> tables, String question) {
        if (question == null || question.isBlank()) {
            return tables;
        }

        ModelConfigBean embeddingModel = modelConfigMapper.selectOne(
                new LambdaQueryWrapper<ModelConfigBean>()
                        .eq(ModelConfigBean::getFunctionType, "embedding")
                        .eq(ModelConfigBean::getIsDel, 0)
                        .orderByDesc(ModelConfigBean::getId)
                        .last("LIMIT 1")
        );

        if (embeddingModel == null) {
            log.warn("No embedding model configured, using all tables");
            return tables;
        }

        List<Float> questionEmbedding;
        try {
            questionEmbedding = aiGatewayChatService.embedding(
                    ChatRequest.builder()
                            .apiKey(embeddingModel.getApiKey())
                            .baseUrl(embeddingModel.getBaseUrl())
                            .model(embeddingModel.getModel())
                            .type(embeddingModel.getType())
                            .question(question)
                            .dimensions(embeddingModel.getDimension())
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate question embedding: {}", e.getMessage());
            return tables;
        }

        if (questionEmbedding == null || questionEmbedding.isEmpty()) {
            log.warn("Failed to generate question embedding, using all tables");
            return tables;
        }

        List<Map.Entry<DataTableBean, Double>> tableSimilarities = new ArrayList<>();

        for (DataTableBean table : tables) {
            if (table.getEmbedding() == null || table.getEmbedding().isEmpty()) {
                continue;
            }

            try {
                List<Float> tableEmbedding = JSONUtil.toList(table.getEmbedding(), Float.class);
                double similarity = calculateCosineSimilarity(questionEmbedding, tableEmbedding);
                
                double boostedSimilarity = applyTableTypeBoost(table.getTable(), question, similarity);
                tableSimilarities.add(new AbstractMap.SimpleEntry<>(table, boostedSimilarity));
            } catch (Exception e) {
                log.warn("Failed to parse embedding for table {}: {}", table.getTable(), e.getMessage());
            }
        }

        if (tableSimilarities.isEmpty()) {
            log.warn("No tables have embeddings, using all tables");
            return tables;
        }

        tableSimilarities.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        int topK = Math.min(DEFAULT_TOP_K, tableSimilarities.size());
        List<DataTableBean> selectedTables = tableSimilarities.stream()
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        log.info("Selected {} tables based on embedding similarity: {}", 
                selectedTables.size(), 
                selectedTables.stream().map(DataTableBean::getTable).collect(Collectors.toList()));

        return selectedTables;
    }

    private double applyTableTypeBoost(String tableName, String question, double similarity) {
        boolean isDimensionTable = tableName.startsWith("dims_");
        boolean isFactTable = tableName.startsWith("facts_");
        
        String lowerQuestion = question.toLowerCase();
        
        String[] dimensionKeywords = {"有哪些", "所有", "列表", "品类", "名称", "价格", "成本", "店长", "门店数量", "商品种类"};
        String[] factKeywords = {"销售", "订单", "金额", "记录", "数量", "折扣"};
        
        boolean needsDimensionTable = Arrays.stream(dimensionKeywords).anyMatch(lowerQuestion::contains);
        boolean needsFactTable = Arrays.stream(factKeywords).anyMatch(lowerQuestion::contains);
        
        if (needsDimensionTable && isDimensionTable) {
            return similarity + 0.15;
        }
        
        if (needsFactTable && isFactTable) {
            return similarity + 0.10;
        }
        
        if (needsDimensionTable && isFactTable && similarity < 0.6) {
            return similarity - 0.10;
        }
        
        return similarity;
    }

    private double calculateCosineSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1 == null || vector2 == null || vector1.isEmpty() || vector2.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        int minLength = Math.min(vector1.size(), vector2.size());
        for (int i = 0; i < minLength; i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += vector1.get(i) * vector1.get(i);
            norm2 += vector2.get(i) * vector2.get(i);
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private String getMainTableName(DataSetBean ds) {
        if ("excel".equalsIgnoreCase(ds.getType())) {
            return ds.getDsName();
        }
        List<DataTableBean> tables = dataTableMapper.selectList(new LambdaQueryWrapper<DataTableBean>()
                .eq(DataTableBean::getDatasetId, ds.getId()));
        return tables.isEmpty() ? ds.getDatabase() : tables.get(0).getTable();
    }

    private DataSetBean findDataSetByTable(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return null;
        }
        List<DataTableBean> tables = dataTableMapper.selectList(
                new LambdaQueryWrapper<DataTableBean>()
                        .eq(DataTableBean::getTable, tableName)
        );
        if (tables.isEmpty()) {
            return null;
        }
        return dataSetMapper.selectById(tables.get(0).getDatasetId());
    }

    private String getTableName(DataSetBean ds) {
        if ("excel".equalsIgnoreCase(ds.getType())) {
            return ds.getDsName();
        }
        List<DataTableBean> tables = dataTableMapper.selectList(new LambdaQueryWrapper<DataTableBean>()
                .eq(DataTableBean::getDatasetId, ds.getId()));
        return tables.isEmpty() ? ds.getDatabase() : tables.get(0).getTable();
    }

    private String buildTableRef(DataSetBean currentDs, DataSetBean relatedDs, String tableName) {
        if (relatedDs == null) {
            return tableName;
        }
        boolean sameDb = isSameDatabase(currentDs, relatedDs);
        if (sameDb) {
            return tableName;
        }
        String dbName = relatedDs.getDatabase();
        if (dbName != null && !dbName.isBlank()) {
            return dbName + "." + tableName;
        }
        return tableName;
    }

    private boolean isSameDatabase(DataSetBean ds1, DataSetBean ds2) {
        if (ds1 == null || ds2 == null) {
            return true;
        }
        boolean sameHost = isSame(ds1.getHost(), ds2.getHost());
        boolean samePort = isSame(ds1.getPort(), ds2.getPort());
        boolean sameDb = isSame(ds1.getDatabase(), ds2.getDatabase());
        return sameHost && samePort && sameDb;
    }

    private boolean isSame(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.toString().equals(b.toString());
    }

    private static class RelationMeta {
        private final DataSetBean relatedDataset;
        private final String fromTable;
        private final String fromField;
        private final String toTable;
        private final String toField;
        private final String joinType;
        private final boolean currentFrom;

        RelationMeta(DataSetBean relatedDataset, String fromTable, String fromField,
                     String toTable, String toField, String joinType, boolean currentFrom) {
            this.relatedDataset = relatedDataset;
            this.fromTable = fromTable;
            this.fromField = fromField;
            this.toTable = toTable;
            this.toField = toField;
            this.joinType = joinType;
            this.currentFrom = currentFrom;
        }

        DataSetBean getRelatedDataset() { return relatedDataset; }
        String getFromTable() { return fromTable; }
        String getFromField() { return fromField; }
        String getToTable() { return toTable; }
        String getToField() { return toField; }
        String getJoinType() { return joinType; }
        boolean isCurrentFrom() { return currentFrom; }
    }

    public String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public void streamPrint(Sinks.Many<ServerSentEvent<WebResult<String>>> sink, String node, String content) {
        sink.tryEmitNext(buildSseEvent(node, WebResult.success(content)));
    }
    
    public void streamPrint(Sinks.Many<ServerSentEvent<WebResult<String>>> sink, String node, String content, SerializableSink serializableSink) {
        sink.tryEmitNext(buildSseEvent(node, WebResult.success(content)));
    }
    
    public void streamPrint(Sinks.Many<ServerSentEvent<WebResult<String>>> sink, String node, String content, SerializableSink serializableSink, WorkflowState workflowState) {
        sink.tryEmitNext(buildSseEvent(node, WebResult.success(content)));
        if (workflowState != null && content != null) {
            workflowState.appendCollectedData(content);
        }
    }

    public void streamPrint(Sinks.Many<ServerSentEvent<WebResult<String>>> sink, String node, String content, QuestionForm questionForm) {
        questionForm.setAnswer(ObjectUtils.isEmpty(questionForm.getAnswer()) ? content : questionForm.getAnswer() + content);
        sink.tryEmitNext(buildSseEvent(node, WebResult.success(content)));
    }
    
    public void errorHandling(String node, Sinks.Many<ServerSentEvent<WebResult<String>>> sink, String errorMsg) {
        ServerSentEvent<WebResult<String>> build = buildSseEvent(node, WebResult.success( errorMsg));
        sink.emitNext(build, Sinks.EmitFailureHandler.FAIL_FAST);
        sink.tryEmitComplete();
    }
    
    public ServerSentEvent<WebResult<String>> buildSseEvent(String node, WebResult<String> data) {
        return ServerSentEvent.<WebResult<String>>builder()
                .id(node)
                .data(data)
                .build();
    }

    public String extractContentAfterMarker(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        final String MARKER = "</think>";
        int markerIndex = input.indexOf(MARKER);

        if (markerIndex != -1) {
            return input.substring(markerIndex + MARKER.length()).trim();
        } else {
            return input;
        }
    }
}