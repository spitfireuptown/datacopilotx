package com.datacopilotx.aigateway.service.embedding;

import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONUtil;
import com.datacopilotx.aigateway.domain.dto.ElasticSearchVectorData;
import com.datacopilotx.aigateway.domain.dto.OllamaResultDTO;
import com.datacopilotx.common.exception.DataCopilotXException;
import com.datacopilotx.common.result.ResponseCode;
import com.datacopilotx.common.util.WorkflowUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.query.functionscore.ScriptScoreQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
public class ElasticSearchVectorStorage {

    @Autowired
    RestHighLevelClient esClient;


    /**
     * 初始化向量数据库index
     * @param dim 维度
     */
    public void initIndex(String indexName, int dim) {
        indexName = WorkflowUtil.innerIndexName(indexName);
        try {
            // 查看向量索引是否存在，此方法为固定默认索引字段
            IndicesClient indices = esClient.indices();
            GetIndexRequest getIndexRequest = new GetIndexRequest(indexName);
            if (!indices.exists(getIndexRequest, RequestOptions.DEFAULT)) {
                CreateIndexRequest request = new CreateIndexRequest(indexName);
                request.mapping(this.elasticMapping(dim));
                this.esClient.indices().create(request, RequestOptions.DEFAULT);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    // 删除索引
    public void deleteIndex(String indexName) throws IOException {
        indexName = WorkflowUtil.innerIndexName(indexName);
        GetIndexRequest request = new GetIndexRequest(indexName);
        request.local(false);
        request.humanReadable(true);
        request.includeDefaults(false);

        boolean exists = esClient.indices().exists(request, RequestOptions.DEFAULT);
        if (exists) {
            DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(indexName);
            esClient.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
        }
    }

    // 检索向量库
    @SneakyThrows
    public List<OllamaResultDTO.CallBackResult> retrieval(String indexName, List<Float> vectors, Integer topK, Float minScore) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("query_vector", vectors);

            // 计算cos值+1，避免出现负数的情况，得到结果后，实际score值在减1再计算
            Script script = new Script(ScriptType.INLINE, Script.DEFAULT_SCRIPT_LANG, "cosineSimilarity(params.query_vector, 'vector')+1", params);
            ScriptScoreQueryBuilder scriptScoreQueryBuilder = new ScriptScoreQueryBuilder(QueryBuilders.termQuery("enable", true), script);

            String actualIndexName = WorkflowUtil.innerIndexName(indexName);
            SearchRequest searchRequest = new SearchRequest(actualIndexName);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                    .query(scriptScoreQueryBuilder)
                    .from(0)
                    .size(topK);

            searchSourceBuilder.minScore(minScore + 1);
            searchRequest.source(searchSourceBuilder);

            SearchResponse searchResponse = esClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits searchHits = searchResponse.getHits();

            List<OllamaResultDTO.CallBackResult> results = new LinkedList<>();
            for (SearchHit hit : searchHits) {
                try {
                    // 使用ElasticSearchVectorData类进行反序列化，确保类型安全
                    ElasticSearchVectorData vectorData = JSONUtil.toBean(hit.getSourceAsString(), ElasticSearchVectorData.class);

                    // 计算实际相似度分数（减去之前加的1）
                    double actualScore = hit.getScore() - 1;

                    // 构建CallBackResult对象并设置所有必要属性
                    results.add(
                            OllamaResultDTO.CallBackResult.builder()
                                    .score((Math.round(actualScore * 100.0) / 100.0))
                                    .question(vectorData.getQuestion())
                                    .answer(vectorData.getAnswer())
                                    .build()
                    );
                } catch (Exception e) {
                    log.error("Error processing search hit: {}", e.getMessage(), e);
                }
            }
            return results;
        } catch (Exception e) {
            log.error("Vector search failed: {}", e.getMessage(), e);
            throw new DataCopilotXException(ResponseCode.COMPONENT_FAILED, "Failed to search vector database: " + e.getMessage());
        }
    }


    private Map<String, Object> elasticMapping(int dims) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("_class", MapUtil.builder("type", "keyword").put("doc_values", "false").put("index", "false").build());
        properties.put("datasetId", MapUtil.builder("type", "keyword").build());
        properties.put("modelId", MapUtil.builder("type", "keyword").build());
        properties.put("question", MapUtil.builder("type", "keyword").build());
        properties.put("answer", MapUtil.builder("type", "keyword").build());
        properties.put("enable", MapUtil.builder("type", "keyword").build());
        properties.put("ctime", MapUtil.builder("type", "keyword").build());
        properties.put("vector", MapUtil.<String, Object>builder("type", "dense_vector").put("dims", dims).build());
        Map<String, Object> root = new HashMap<>();
        root.put("properties", properties);
        return root;
    }


    public void deleteData(String index, String docId) {
        index = WorkflowUtil.innerIndexName(index);
        DeleteRequest deleteRequest = new DeleteRequest(index, docId);
        try {
            esClient.delete(deleteRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("delete data error, index: {}, docId: {}", index, docId, e);
            throw new DataCopilotXException(ResponseCode.COMPONENT_FAILED, e.getMessage());
        }
    }

    public ElasticSearchVectorData selectOneData(String index, String docId) {
        index = WorkflowUtil.innerIndexName(index);
        GetRequest getRequest = new GetRequest(index, docId);
        try {
            GetResponse getResponse = esClient.get(getRequest, RequestOptions.DEFAULT);
            ElasticSearchVectorData vectorData = JSONUtil.toBean(getResponse.getSourceAsString(), ElasticSearchVectorData.class);
            vectorData.setDocId(getResponse.getId());
            return vectorData;
        } catch (IOException e) {
            log.error("delete data error, index: {}, docId: {}", index, docId, e);
            throw new DataCopilotXException(ResponseCode.COMPONENT_FAILED, e.getMessage());
        }
    }


    public void updateData(String index, String docId, ElasticSearchVectorData ele) {
        index = WorkflowUtil.innerIndexName(index);
        UpdateRequest updateRequest = new UpdateRequest(index, docId);
        try {
            updateRequest.doc(JSONUtil.toJsonStr(ele), XContentType.JSON);
            esClient.update(updateRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("update data error, index: {}, docId: {}", index, docId, e);
            throw new DataCopilotXException(ResponseCode.COMPONENT_FAILED, e.getMessage());
        }
    }

    // 存储向量库
    @SneakyThrows
    public void storeData(String indexName, ElasticSearchVectorData ele) {
        indexName = WorkflowUtil.innerIndexName(indexName);
        IndexRequest indexRequest = new IndexRequest(indexName).source(JSONUtil.toJsonStr(ele), XContentType.JSON);

        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.add(indexRequest);

        BulkResponse bulkResponse = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        if (bulkResponse.hasFailures()) {
            log.error("store data save error: {}", bulkResponse.buildFailureMessage());
            throw new DataCopilotXException(ResponseCode.COMPONENT_FAILED, bulkResponse.buildFailureMessage());
        }
    }

    /**
     * 查询指定index下的所有数据
     * @return 查询到的向量数据列表
     */
    @SneakyThrows
    public ElasticSearchVectorData.ElasticSearchVectorPageDTO pageIndexData(ElasticSearchVectorData.ElasticSearchFormDTO elasticSearchFormDTO) {
        String indexName = elasticSearchFormDTO.getIndexName();
    
        indexName = WorkflowUtil.innerIndexName(indexName);
        ElasticSearchVectorData.ElasticSearchVectorPageDTO elasticSearchVectorPageDTO = new ElasticSearchVectorData.ElasticSearchVectorPageDTO();
    
        IndicesClient indices = esClient.indices();
        GetIndexRequest getIndexRequest = new GetIndexRequest(indexName);
        if (!indices.exists(getIndexRequest, RequestOptions.DEFAULT)) {
            log.info("Index {} does not exist", indexName);
            return elasticSearchVectorPageDTO;
        }
    
        SearchRequest searchRequest = new SearchRequest();
        //索引名称
        searchRequest.indices(indexName);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //根据ID进行排序
    //        sourceBuilder.sort("_id");
        //分页
        sourceBuilder.from(elasticSearchFormDTO.getPageNo());
        sourceBuilder.size(elasticSearchFormDTO.getPageSize());
        BoolQueryBuilder query = new BoolQueryBuilder();
        sourceBuilder.query(query);
    
        QueryBuilder queryBuilder = QueryBuilders.matchAllQuery();
        //匹配策略
        query.must(queryBuilder);
        
        // 增加name字段筛选条件
        if (elasticSearchFormDTO.getName() != null && !elasticSearchFormDTO.getName().isEmpty()) {
            query.must(QueryBuilders.matchQuery("name", elasticSearchFormDTO.getName()));
        }
        
        // 增加enable字段筛选条件
        if (elasticSearchFormDTO.getEnable() != null) {
            query.must(QueryBuilders.termQuery("enable", elasticSearchFormDTO.getEnable()));
        }
        
        searchRequest.source(sourceBuilder);
        SearchResponse rp = esClient.search(searchRequest, RequestOptions.DEFAULT);
        List<ElasticSearchVectorData> resultList = new ArrayList<>();
        for (SearchHit hit : rp.getHits()) {
            ElasticSearchVectorData vectorData = JSONUtil.toBean(hit.getSourceAsString(), ElasticSearchVectorData.class);
            vectorData.setDocId(hit.getId());
            resultList.add(vectorData);
        }
        elasticSearchVectorPageDTO.setData(resultList);
        elasticSearchVectorPageDTO.setTotal(Objects.requireNonNull(rp.getHits().getTotalHits()).value);
        return elasticSearchVectorPageDTO;
    }
}
