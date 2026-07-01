package com.datacopilotx.aigateway.service.embedding;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONUtil;
import com.datacopilotx.aigateway.domain.dto.ElasticSearchVectorData;
import com.datacopilotx.aigateway.domain.dto.OllamaResultDTO;
import com.datacopilotx.common.exception.DataCopilotXException;
import com.datacopilotx.common.result.ResponseCode;
import com.datacopilotx.common.util.WorkflowUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
public class ElasticSearchVectorStorage {

    @Autowired
    ElasticsearchClient esClient;

    private static final int RRF_K = 60;


    public void initIndex(String indexName, int dim) {
        indexName = WorkflowUtil.innerIndexName(indexName);
        try {
            GetIndexRequest getIndexRequest = new GetIndexRequest.Builder().index(indexName).build();
            boolean exists = false;
            try {
                GetIndexResponse response = esClient.indices().get(getIndexRequest);
                exists = response.result().containsKey(indexName);
            } catch (Exception e) {
                exists = false;
            }

            if (!exists) {
                CreateIndexRequest request = new CreateIndexRequest.Builder()
                        .index(indexName)
                        .mappings(m -> m
                                .properties("_class", p -> p.keyword(k -> k.docValues(false).index(false)))
                                .properties("datasetId", p -> p.keyword(k -> k))
                                .properties("modelId", p -> p.keyword(k -> k))
                                .properties("question", p -> p.text(t -> t.analyzer("standard")))
                                .properties("question_kw", p -> p.keyword(k -> k))
                                .properties("answer", p -> p.text(t -> t.analyzer("standard")))
                                .properties("answer_kw", p -> p.keyword(k -> k))
                                .properties("enable", p -> p.keyword(k -> k))
                                .properties("ctime", p -> p.keyword(k -> k))
                                .properties("vector", p -> p.denseVector(d -> d.dims(dim).index(true).similarity("cosine")))
                        )
                        .build();

                CreateIndexResponse response = esClient.indices().create(request);
                log.info("Created index {}: {}", indexName, response.acknowledged());
            }
        } catch (Exception e) {
            log.error("Failed to create index: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public void deleteIndex(String indexName) throws IOException {
        indexName = WorkflowUtil.innerIndexName(indexName);
        GetIndexRequest getIndexRequest = new GetIndexRequest.Builder().index(indexName).build();
        boolean exists = false;
        try {
            GetIndexResponse response = esClient.indices().get(getIndexRequest);
            exists = response.result().containsKey(indexName);
        } catch (Exception e) {
            exists = false;
        }

        if (exists) {
            DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest.Builder().index(indexName).build();
            esClient.indices().delete(deleteIndexRequest);
        }
    }

    @SneakyThrows
    public List<OllamaResultDTO.CallBackResult> retrieval(String indexName, List<Float> vectors, Integer topK, Float minScore) {
        return hybridRetrieval(indexName, vectors, null, topK, minScore);
    }

    @SneakyThrows
    public List<OllamaResultDTO.CallBackResult> hybridRetrieval(String indexName, List<Float> vectors, String queryText, Integer topK, Float minScore) {
        String actualIndexName = WorkflowUtil.innerIndexName(indexName);

        List<OllamaResultDTO.CallBackResult> vectorResults = knnSearch(actualIndexName, vectors, topK * 2);
        List<OllamaResultDTO.CallBackResult> bm25Results = queryText != null && !queryText.isEmpty()
                ? bm25Search(actualIndexName, queryText, topK * 2)
                : Collections.emptyList();

        Map<String, OllamaResultDTO.CallBackResult> mergedMap = new HashMap<>();
        Map<String, Integer> vectorRankMap = new HashMap<>();
        Map<String, Integer> bm25RankMap = new HashMap<>();

        for (int i = 0; i < vectorResults.size(); i++) {
            var result = vectorResults.get(i);
            mergedMap.put(result.getDocId(), result);
            vectorRankMap.put(result.getDocId(), i + 1);
        }

        for (int i = 0; i < bm25Results.size(); i++) {
            var result = bm25Results.get(i);
            if (!mergedMap.containsKey(result.getDocId())) {
                mergedMap.put(result.getDocId(), result);
            }
            bm25RankMap.put(result.getDocId(), i + 1);
        }

        List<OllamaResultDTO.CallBackResult> finalResults = new ArrayList<>();
        for (var entry : mergedMap.entrySet()) {
            String docId = entry.getKey();
            var result = entry.getValue();

            double rrfScore = 0;
            if (vectorRankMap.containsKey(docId)) {
                rrfScore += 1.0 / (RRF_K + vectorRankMap.get(docId));
            }
            if (bm25RankMap.containsKey(docId)) {
                rrfScore += 1.0 / (RRF_K + bm25RankMap.get(docId));
            }

            finalResults.add(OllamaResultDTO.CallBackResult.builder()
                    .docId(docId)
                    .score(Math.round(rrfScore * 1000.0) / 1000.0)
                    .question(result.getQuestion())
                    .answer(result.getAnswer())
                    .build());
        }

        finalResults.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        return finalResults.stream()
                .filter(r -> r.getScore() >= minScore)
                .limit(topK)
                .collect(java.util.stream.Collectors.toList());
    }

    @SneakyThrows
    private List<OllamaResultDTO.CallBackResult> knnSearch(String indexName, List<Float> vectors, int size) {
        KnnSearch knnSearch = new KnnSearch.Builder()
                .field("vector")
                .queryVector(vectors)
                .numCandidates(size * 5L)
                .build();

        TermQuery enableFilter = new TermQuery.Builder()
                .field("enable")
                .value("true")
                .build();

        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(indexName)
                .knn(knnSearch)
                .query(q -> q.term(enableFilter))
                .size(size)
                .build();

        SearchResponse<ElasticSearchVectorData> searchResponse = esClient.search(searchRequest, ElasticSearchVectorData.class);

        List<OllamaResultDTO.CallBackResult> results = new LinkedList<>();
        for (Hit<ElasticSearchVectorData> hit : searchResponse.hits().hits()) {
            ElasticSearchVectorData vectorData = hit.source();
            if (vectorData != null) {
                results.add(OllamaResultDTO.CallBackResult.builder()
                        .docId(hit.id())
                        .score(Math.round(hit.score() * 100.0) / 100.0)
                        .question(vectorData.getQuestion())
                        .answer(vectorData.getAnswer())
                        .build());
            }
        }
        return results;
    }

    @SneakyThrows
    private List<OllamaResultDTO.CallBackResult> bm25Search(String indexName, String queryText, int size) {
        MatchQuery questionQuery = new MatchQuery.Builder()
                .field("question")
                .query(queryText)
                .boost(1.0f)
                .build();

        MatchQuery answerQuery = new MatchQuery.Builder()
                .field("answer")
                .query(queryText)
                .boost(0.8f)
                .build();

        TermQuery enableFilter = new TermQuery.Builder()
                .field("enable")
                .value("true")
                .build();

        BoolQuery boolQuery = new BoolQuery.Builder()
                .filter(f -> f.term(enableFilter))
                .should(s -> s.match(questionQuery))
                .should(s -> s.match(answerQuery))
                .minimumShouldMatch("1")
                .build();

        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(indexName)
                .query(q -> q.bool(boolQuery))
                .size(size)
                .build();

        SearchResponse<ElasticSearchVectorData> searchResponse = esClient.search(searchRequest, ElasticSearchVectorData.class);

        List<OllamaResultDTO.CallBackResult> results = new LinkedList<>();
        for (Hit<ElasticSearchVectorData> hit : searchResponse.hits().hits()) {
            ElasticSearchVectorData vectorData = hit.source();
            if (vectorData != null) {
                results.add(OllamaResultDTO.CallBackResult.builder()
                        .docId(hit.id())
                        .score(Math.round(hit.score() * 100.0) / 100.0)
                        .question(vectorData.getQuestion())
                        .answer(vectorData.getAnswer())
                        .build());
            }
        }
        return results;
    }


    public void deleteData(String index, String docId) {
        index = WorkflowUtil.innerIndexName(index);
        try {
            DeleteRequest deleteRequest = new DeleteRequest.Builder()
                    .index(index)
                    .id(docId)
                    .build();
            esClient.delete(deleteRequest);
        } catch (IOException e) {
            log.error("delete data error, index: {}, docId: {}", index, docId, e);
            throw new DataCopilotXException(ResponseCode.COMPONENT_FAILED, e.getMessage());
        }
    }

    public ElasticSearchVectorData selectOneData(String index, String docId) {
        index = WorkflowUtil.innerIndexName(index);
        try {
            GetRequest getRequest = new GetRequest.Builder()
                    .index(index)
                    .id(docId)
                    .build();
            GetResponse<ElasticSearchVectorData> getResponse = esClient.get(getRequest, ElasticSearchVectorData.class);
            ElasticSearchVectorData vectorData = getResponse.source();
            if (vectorData != null) {
                vectorData.setDocId(getResponse.id());
            }
            return vectorData;
        } catch (IOException e) {
            log.error("select data error, index: {}, docId: {}", index, docId, e);
            throw new DataCopilotXException(ResponseCode.COMPONENT_FAILED, e.getMessage());
        }
    }


    public void updateData(String index, String docId, ElasticSearchVectorData ele) {
        index = WorkflowUtil.innerIndexName(index);
        try {
            Map<String, Object> source = JSONUtil.toBean(JSONUtil.toJsonStr(ele), Map.class);
            source.put("question_kw", ele.getQuestion());
            source.put("answer_kw", ele.getAnswer());

            String finalIndex = index;
            esClient.update(u -> u
                    .index(finalIndex)
                    .id(docId)
                    .doc(source),
                    Map.class
            );
        } catch (IOException e) {
            log.error("update data error, index: {}, docId: {}", index, docId, e);
            throw new DataCopilotXException(ResponseCode.COMPONENT_FAILED, e.getMessage());
        }
    }

    @SneakyThrows
    public void storeData(String indexName, ElasticSearchVectorData ele) {
        indexName = WorkflowUtil.innerIndexName(indexName);
        Map<String, Object> source = JSONUtil.toBean(JSONUtil.toJsonStr(ele), Map.class);
        source.put("question_kw", ele.getQuestion());
        source.put("answer_kw", ele.getAnswer());

        IndexOperation<Map<String, Object>> indexOp = new IndexOperation.Builder<Map<String, Object>>()
                .index(indexName)
                .document(source)
                .build();

        BulkRequest bulkRequest = new BulkRequest.Builder()
                .operations(BulkOperation.of(o -> o.index(indexOp)))
                .build();

        BulkResponse bulkResponse = esClient.bulk(bulkRequest);
        if (bulkResponse.errors()) {
            log.error("store data save error: {}", bulkResponse.errors());
            throw new DataCopilotXException(ResponseCode.COMPONENT_FAILED, "Bulk operation failed");
        }
    }

    @SneakyThrows
    public ElasticSearchVectorData.ElasticSearchVectorPageDTO pageIndexData(ElasticSearchVectorData.ElasticSearchFormDTO elasticSearchFormDTO) {
        String indexName = elasticSearchFormDTO.getIndexName();

        indexName = WorkflowUtil.innerIndexName(indexName);
        ElasticSearchVectorData.ElasticSearchVectorPageDTO elasticSearchVectorPageDTO = new ElasticSearchVectorData.ElasticSearchVectorPageDTO();

        try {
            GetIndexRequest getIndexRequest = new GetIndexRequest.Builder().index(indexName).build();
            GetIndexResponse response = esClient.indices().get(getIndexRequest);
            if (!response.result().containsKey(indexName)) {
                log.info("Index {} does not exist", indexName);
                return elasticSearchVectorPageDTO;
            }
        } catch (Exception e) {
            log.info("Index {} does not exist", indexName);
            return elasticSearchVectorPageDTO;
        }

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        if (elasticSearchFormDTO.getName() != null && !elasticSearchFormDTO.getName().isEmpty()) {
            MatchQuery nameQuery = new MatchQuery.Builder()
                    .field("name")
                    .query(elasticSearchFormDTO.getName())
                    .build();
            boolQueryBuilder.must(m -> m.match(nameQuery));
        }

        if (elasticSearchFormDTO.getEnable() != null) {
            TermQuery enableQuery = new TermQuery.Builder()
                    .field("enable")
                    .value(elasticSearchFormDTO.getEnable().toString())
                    .build();
            boolQueryBuilder.must(m -> m.term(enableQuery));
        }

        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(indexName)
                .query(q -> q.bool(boolQueryBuilder.build()))
                .from(elasticSearchFormDTO.getPageNo())
                .size(elasticSearchFormDTO.getPageSize())
                .build();

        SearchResponse<ElasticSearchVectorData> rp = esClient.search(searchRequest, ElasticSearchVectorData.class);

        List<ElasticSearchVectorData> resultList = new ArrayList<>();
        for (Hit<ElasticSearchVectorData> hit : rp.hits().hits()) {
            ElasticSearchVectorData vectorData = hit.source();
            if (vectorData != null) {
                vectorData.setDocId(hit.id());
                resultList.add(vectorData);
            }
        }
        elasticSearchVectorPageDTO.setData(resultList);
        elasticSearchVectorPageDTO.setTotal(rp.hits().total().value());
        return elasticSearchVectorPageDTO;
    }
}