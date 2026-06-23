package com.datacopilotx.ai.service.graph.main;

import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datacopilotx.ai.controller.form.QuestionForm;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.DatasetRelationBean;
import com.datacopilotx.ai.domian.dto.DataSetDTO;
import com.datacopilotx.ai.mapper.DataSetMapper;
import com.datacopilotx.ai.mapper.DatasetRelationMapper;
import com.datacopilotx.common.result.WebResult;
import jakarta.annotation.Resource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 流程服务辅助类，提供公共方法
 */
@Component
public class WorkflowServiceHelper {

    @Resource
    private DataSetMapper dataSetMapper;

    @Resource
    private DatasetRelationMapper datasetRelationMapper;
    
    // 替换prompt占位符
    public Pair<String, String> injectPrompt(Pair<String, String> promptPair, Map<String, String> params) {
        String systemPrompt = promptPair.getKey();
        String userPrompt = promptPair.getValue();

        for (Map.Entry<String, String> stringStringEntry : params.entrySet()) {
            systemPrompt = systemPrompt.replace(stringStringEntry.getKey(), stringStringEntry.getValue());
            userPrompt = userPrompt.replace(stringStringEntry.getKey(), stringStringEntry.getValue());
        }
        return new Pair<>(systemPrompt, userPrompt);
    }

    // 组装数据集信息（包含关联表元数据）
    public String assembleDataSetInfo(DataSetBean dataSetBean) {
        List<DataSetDTO.SchemaInfo> schemaInfos = JSONUtil.toList(dataSetBean.getFields(), DataSetDTO.SchemaInfo.class);

        String tableName = dataSetBean.getTable();
        if ("excel".equalsIgnoreCase(dataSetBean.getType())) {
            tableName = dataSetBean.getDsName();
        }

        StringBuilder result = new StringBuilder();

        // 主表信息
        result.append(String.format("**主表名:** %s\n", tableName));
        result.append(String.format("**主表描述:** %s\n", dataSetBean.getDescription() != null ? dataSetBean.getDescription() : ""));
        result.append("**主表字段:**\n");
        for (DataSetDTO.SchemaInfo field : schemaInfos) {
            result.append(String.format("    - 字段名：%s | 类型：%s | 描述：%s\n",
                    field.getFieldName(), field.getFieldType(),
                    field.getDescription() != null ? field.getDescription() : ""));
        }

        // 收集所有关联关系（来自 DATA_SET_RELATION 表和 DATA_SET.relations 字段）
        List<RelationMeta> relationMetas = new ArrayList<>();

        // 1. 从 DATA_SET_RELATION 表查询关联关系
        List<DatasetRelationBean> dbRelations = datasetRelationMapper.selectByDatasetId(dataSetBean.getId());
        if (dbRelations != null && !dbRelations.isEmpty()) {
            for (DatasetRelationBean rel : dbRelations) {
                Long relatedDatasetId = rel.getFromDatasetId().equals(dataSetBean.getId())
                        ? rel.getToDatasetId() : rel.getFromDatasetId();
                DataSetBean relatedDs = dataSetMapper.selectById(relatedDatasetId);
                if (relatedDs != null && relatedDs.getIsDel() == 0) {
                    relationMetas.add(new RelationMeta(
                            relatedDs,
                            rel.getFromField(),
                            rel.getToField(),
                            rel.getRelationType(),
                            rel.getFromDatasetId().equals(dataSetBean.getId()) // 当前表是 from 还是 to
                    ));
                }
            }
        }

        // 2. 从 DATA_SET.relations JSON 字段解析（兼容旧数据）
        String relationsStr = dataSetBean.getRelations();
        if (relationsStr != null && !relationsStr.isBlank()) {
            List<DataSetDTO.TableRelation> jsonRelations = JSONUtil.toList(relationsStr, DataSetDTO.TableRelation.class);
            if (jsonRelations != null && !jsonRelations.isEmpty()) {
                for (DataSetDTO.TableRelation tr : jsonRelations) {
                    // 只添加未在 DB 关系中出现的
                    boolean alreadyExists = relationMetas.stream().anyMatch(rm ->
                            rm.getJoinType().equals(tr.getRelationType() != null ? tr.getRelationType() : "INNER JOIN"));
                    if (!alreadyExists || relationMetas.isEmpty()) {
                        // 尝试通过表名查找关联数据集
                        DataSetBean relatedDs = findDataSetByTable(tr.getToTable());
                        if (relatedDs != null) {
                            relationMetas.add(new RelationMeta(
                                    relatedDs, tr.getFromField(), tr.getToField(),
                                    tr.getRelationType() != null ? tr.getRelationType() : "INNER JOIN", true));
                        }
                    }
                }
            }
        }

        // 输出关联表信息
        if (!relationMetas.isEmpty()) {
            result.append("\n**联表关系:**\n");
            for (RelationMeta rm : relationMetas) {
                String joinType = rm.getJoinType() != null ? rm.getJoinType() : "INNER JOIN";
                String relatedTableName = getTableName(rm.getRelatedDataset());
                String fromTable = rm.isCurrentFrom() ? tableName : relatedTableName;
                String fromField = rm.isCurrentFrom() ? rm.getFromField() : rm.getToField();
                String toTable = rm.isCurrentFrom() ? relatedTableName : tableName;
                String toField = rm.isCurrentFrom() ? rm.getToField() : rm.getFromField();

                // 跨库时使用 database.table 格式
                String fromTableRef = buildTableRef(dataSetBean, rm.isCurrentFrom() ? null : rm.getRelatedDataset(), fromTable);
                String toTableRef = buildTableRef(dataSetBean, rm.isCurrentFrom() ? rm.getRelatedDataset() : null, toTable);

                result.append(String.format("    %s %s ON %s.%s = %s.%s\n",
                        joinType, toTableRef, fromTableRef, fromField, toTableRef, toField));

                // 输出关联表字段
                List<DataSetDTO.SchemaInfo> relFields = JSONUtil.toList(rm.getRelatedDataset().getFields(), DataSetDTO.SchemaInfo.class);
                result.append(String.format("    **关联表 [%s] 字段:**\n", toTableRef));
                for (DataSetDTO.SchemaInfo field : relFields) {
                    result.append(String.format("        - 字段名：%s | 类型：%s | 描述：%s\n",
                            field.getFieldName(), field.getFieldType(),
                            field.getDescription() != null ? field.getDescription() : ""));
                }
            }
        }

        return result.toString();
    }

    /**
     * 通过表名查找数据集
     */
    private DataSetBean findDataSetByTable(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return null;
        }
        List<DataSetBean> list = dataSetMapper.selectList(
                new LambdaQueryWrapper<DataSetBean>()
                        .eq(DataSetBean::getTable, tableName)
        );
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 获取数据集的表名
     */
    private String getTableName(DataSetBean ds) {
        if ("excel".equalsIgnoreCase(ds.getType())) {
            return ds.getDsName();
        }
        return ds.getTable();
    }

    /**
     * 构建表引用（跨库时使用 database.table 格式）
     * @param currentDs 当前数据集
     * @param relatedDs 关联数据集（null 表示使用当前数据集）
     * @param tableName 表名
     */
    private String buildTableRef(DataSetBean currentDs, DataSetBean relatedDs, String tableName) {
        if (relatedDs == null) {
            // 当前数据集，直接使用表名
            return tableName;
        }
        // 检查是否跨库
        boolean sameDb = isSameDatabase(currentDs, relatedDs);
        if (sameDb) {
            return tableName;
        }
        // 跨库时使用 database.table 格式
        String dbName = relatedDs.getDatabase();
        if (dbName != null && !dbName.isBlank()) {
            return dbName + "." + tableName;
        }
        return tableName;
    }

    /**
     * 判断两个数据集是否在同一数据库
     */
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

    /**
     * 关联关系元数据内部类
     */
    private static class RelationMeta {
        private final DataSetBean relatedDataset;
        private final String fromField;
        private final String toField;
        private final String joinType;
        private final boolean currentFrom; // 当前数据集是否为 from 方

        RelationMeta(DataSetBean relatedDataset, String fromField, String toField,
                     String joinType, boolean currentFrom) {
            this.relatedDataset = relatedDataset;
            this.fromField = fromField;
            this.toField = toField;
            this.joinType = joinType;
            this.currentFrom = currentFrom;
        }

        DataSetBean getRelatedDataset() { return relatedDataset; }
        String getFromField() { return fromField; }
        String getToField() { return toField; }
        String getJoinType() { return joinType; }
        boolean isCurrentFrom() { return currentFrom; }
    }

    // 获取当前时间
    public String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // 输出问题和答案（内部方法）
    public void streamPrint(Sinks.Many<ServerSentEvent<WebResult<String>>> sink, String node, String content) {
        sink.tryEmitNext(buildSseEvent(node, WebResult.success(content)));
    }
    
    // 输出问题和答案（同时收集数据用于保存到question_log）
    public void streamPrint(Sinks.Many<ServerSentEvent<WebResult<String>>> sink, String node, String content, SerializableSink serializableSink) {
        sink.tryEmitNext(buildSseEvent(node, WebResult.success(content)));
    }
    
    // 输出问题和答案（同时收集数据到WorkflowState用于保存到question_log）
    public void streamPrint(Sinks.Many<ServerSentEvent<WebResult<String>>> sink, String node, String content, SerializableSink serializableSink, WorkflowState workflowState) {
        sink.tryEmitNext(buildSseEvent(node, WebResult.success(content)));
        if (workflowState != null && content != null) {
            workflowState.appendCollectedData(content);
        }
    }

    // 输出问题和答案（内部方法）
    public void streamPrint(Sinks.Many<ServerSentEvent<WebResult<String>>> sink, String node, String content, QuestionForm questionForm) {
        questionForm.setAnswer(ObjectUtils.isEmpty(questionForm.getAnswer()) ? content : questionForm.getAnswer() + content);
        sink.tryEmitNext(buildSseEvent(node, WebResult.success(content)));
    }
    
    // 异常处理
    public void errorHandling(String node, Sinks.Many<ServerSentEvent<WebResult<String>>> sink, String errorMsg) {
        ServerSentEvent<WebResult<String>> build = buildSseEvent(node, WebResult.success( errorMsg));
        sink.emitNext(build, Sinks.EmitFailureHandler.FAIL_FAST);
        sink.tryEmitComplete();
    }
    
    // 构建SSE事件
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
            // 找到标记，返回标记之后的内容（包括换行）
            return input.substring(markerIndex + MARKER.length()).trim();
        } else {
            // 没有找到标记，返回空字符串
            return input;
        }
    }
}