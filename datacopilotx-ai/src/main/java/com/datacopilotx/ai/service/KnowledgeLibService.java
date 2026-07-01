package com.datacopilotx.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datacopilotx.ai.controller.form.KnowledgeLibForm;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.KnowledgeLibBean;
import com.datacopilotx.ai.domian.bean.ModelConfigBean;
import com.datacopilotx.ai.domian.dto.QueryDTO;
import com.datacopilotx.ai.domian.vo.KnowledgeLibVO;
import com.datacopilotx.ai.domian.vo.PageVO;
import com.datacopilotx.ai.domian.vo.UserInfoVo;
import com.datacopilotx.ai.mapper.DataSetMapper;
import com.datacopilotx.ai.mapper.KnowledgeLibMapper;
import com.datacopilotx.ai.mapper.ModelConfigMapper;
import com.datacopilotx.aigateway.domain.dto.ChatRequest;
import com.datacopilotx.aigateway.domain.dto.ElasticSearchVectorData;
import com.datacopilotx.aigateway.domain.dto.OllamaResultDTO;
import com.datacopilotx.aigateway.service.chat.AIGatewayChatService;
import com.datacopilotx.aigateway.service.embedding.ElasticSearchVectorStorage;
import com.datacopilotx.common.exception.DataCopilotXException;
import com.datacopilotx.common.result.ResponseCode;
import com.datacopilotx.common.util.IdUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import com.datacopilotx.ai.util.SecurityUtil;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeLibService {
    @Autowired
    AIGatewayChatService aiGatewayChatService;
    @Autowired
    ElasticSearchVectorStorage elasticSearchVectorStorage;
    @Autowired
    ModelConfigMapper modelConfigMapper;
    @Autowired
    DataSetMapper dataSetMapper;
    @Autowired
    KnowledgeLibMapper knowledgeLibMapper;
    @Autowired
    AuthService authService;

    public List<KnowledgeLibVO.List> list() {
        return knowledgeLibMapper.selectList(
                        new LambdaQueryWrapper<KnowledgeLibBean>()
                                .select(KnowledgeLibBean::getId, KnowledgeLibBean::getName, KnowledgeLibBean::getDatasetId, KnowledgeLibBean::getModelId, KnowledgeLibBean::getDescription, KnowledgeLibBean::getCreator))
                .stream()
                .map(item -> {
                    String creatorName = "";
                    if (item.getCreator() != null) {
                        try {
                            UserInfoVo user = authService.getUserById(item.getCreator());
                            creatorName = user != null ? user.getNickname() : "";
                        } catch (Exception e) {
                            creatorName = "";
                        }
                    }
                    return KnowledgeLibVO.List.builder()
                            .id(item.getId())
                            .name(item.getName())
                            .datasetId(item.getDatasetId())
                            .modelId(item.getModelId())
                            .description(item.getDescription())
                            .creator(item.getCreator())
                            .creatorName(creatorName)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(KnowledgeLibForm.CreateForm createForm) {
        KnowledgeLibBean libNameCheck = knowledgeLibMapper.selectOne(new LambdaQueryWrapper<KnowledgeLibBean>().eq(KnowledgeLibBean::getName, createForm.getName()));
        if (!ObjectUtils.isEmpty(libNameCheck)) {
            throw new DataCopilotXException(ResponseCode.DATASET_REPEAT);
        }

        ModelConfigBean modelConfigBean = modelConfigMapper.selectOne(new LambdaQueryWrapper<ModelConfigBean>().eq(ModelConfigBean::getId, createForm.getModelId()));
        if (ObjectUtils.isEmpty(modelConfigBean)) {
            throw new DataCopilotXException(ResponseCode.MODEL_NOT_FOUND);
        }

        DataSetBean dataSetBean = dataSetMapper.selectOne(new LambdaQueryWrapper<DataSetBean>().eq(DataSetBean::getId, createForm.getDatasetId()));
        if (ObjectUtils.isEmpty(dataSetBean)) {
            throw new DataCopilotXException(ResponseCode.DATASET_NOT_FOUND);
        }

        KnowledgeLibBean knowledgeLibBean = new KnowledgeLibBean();
        knowledgeLibBean.setName(createForm.getName());
        knowledgeLibBean.setDatasetId(createForm.getDatasetId());
        knowledgeLibBean.setModelId(createForm.getModelId());
        knowledgeLibBean.setDescription(createForm.getDescription());
        knowledgeLibBean.setCreator(SecurityUtil.getCurrentUserId());
        knowledgeLibMapper.insert(knowledgeLibBean);

        elasticSearchVectorStorage.initIndex(createForm.getName(), modelConfigBean.getDimension());
        return knowledgeLibBean.getId();
    }


    @Transactional(rollbackFor = Exception.class)
    public Long update(KnowledgeLibForm.UpdateForm updateForm) {
        KnowledgeLibBean libNameCheck = knowledgeLibMapper.selectOne(new LambdaQueryWrapper<KnowledgeLibBean>().eq(KnowledgeLibBean::getName, updateForm.getName()).ne(KnowledgeLibBean::getId, updateForm.getId()));
        if (!ObjectUtils.isEmpty(libNameCheck)) {
            throw new DataCopilotXException(ResponseCode.DATASET_REPEAT);
        }

        KnowledgeLibBean knowledgeLibBean = knowledgeLibMapper.selectById(updateForm.getId());
        if (ObjectUtils.isEmpty(knowledgeLibBean)) {
            throw new DataCopilotXException(ResponseCode.KNOWLEDGE_LIB_NOT_FOUND);
        }

        if (!Objects.equals(updateForm.getModelId(), knowledgeLibBean.getModelId())) {
            throw new DataCopilotXException(ResponseCode.KNOWLEDGE_LIB_CONFIG_ERROR, "知识库禁止修改嵌入模型");
        }

        knowledgeLibBean.setName(updateForm.getName());
        knowledgeLibBean.setDescription(updateForm.getDescription());
        knowledgeLibBean.setDatasetId(updateForm.getDatasetId());
        knowledgeLibMapper.updateById(knowledgeLibBean);
        return knowledgeLibBean.getId();
    }


    /**
     * 存储向量化数据
     */
    public void store(KnowledgeLibForm.StoreForm storeForm) {
        KnowledgeLibBean knowledgeLibBean = knowledgeLibMapper.selectById(storeForm.getKnowledgeLibId());
        if (ObjectUtils.isEmpty(knowledgeLibBean)) {
            throw new DataCopilotXException(ResponseCode.KNOWLEDGE_LIB_NOT_FOUND);
        }

        ElasticSearchVectorData ele = new ElasticSearchVectorData();
        ele.setQuestion(storeForm.getQuestion());
        ele.setAnswer(storeForm.getAnswer());
        ele.setModelId(knowledgeLibBean.getModelId());
        ele.setDatasetId(knowledgeLibBean.getDatasetId());
        ele.setCtime(new Timestamp(System.currentTimeMillis()));
        ele.setEnable(true);

        ModelConfigBean modelConfigBean = modelConfigMapper.selectById(knowledgeLibBean.getModelId());
        if (ObjectUtils.isEmpty(modelConfigBean)) {
            throw new DataCopilotXException(ResponseCode.MODEL_NOT_FOUND);
        }

        List<Float> embedding = aiGatewayChatService.embedding(
                ChatRequest
                        .builder()
                        .apiKey(modelConfigBean.getApiKey())
                        .baseUrl(modelConfigBean.getBaseUrl())
                        .model(modelConfigBean.getModel())
                        .type(modelConfigBean.getType())
                        .question(storeForm.getQuestion())
                        .dimensions(modelConfigBean.getDimension())
                        .build()
        );
        if (ObjectUtils.isEmpty(embedding)) {
            throw new DataCopilotXException(ResponseCode.VECTOR_EMBEDDING_FAILED);
        }
        ele.setVector(embedding);
        elasticSearchVectorStorage.storeData(knowledgeLibBean.getName(), ele);
    }


    public void modify(KnowledgeLibForm.UpdateDateForm updateDateForm) {
        KnowledgeLibBean knowledgeLibBean = knowledgeLibMapper.selectById(updateDateForm.getKnowledgeLibId());
        if (ObjectUtils.isEmpty(knowledgeLibBean)) {
            throw new DataCopilotXException(ResponseCode.KNOWLEDGE_LIB_NOT_FOUND);
        }
        ElasticSearchVectorData elasticSearchVectorData = elasticSearchVectorStorage.selectOneData(knowledgeLibBean.getName(), updateDateForm.getDocId());
        elasticSearchVectorData.setEnable(updateDateForm.getEnable());
        elasticSearchVectorData.setQuestion(updateDateForm.getQuestion());
        elasticSearchVectorData.setAnswer(updateDateForm.getAnswer());
        elasticSearchVectorStorage.updateData(knowledgeLibBean.getName(), updateDateForm.getDocId(), elasticSearchVectorData);
    }

    /**
     * 检索向量化数据（混合检索：向量相似度 + BM25文本匹配）
     */
    public List<OllamaResultDTO.CallBackResult> retrieval(KnowledgeLibForm.RetrievalForm retrievalForm) {
        KnowledgeLibBean knowledgeLibBean = knowledgeLibMapper.selectById(retrievalForm.getKnowledgeLibId());
        if (ObjectUtils.isEmpty(knowledgeLibBean)) {
            throw new DataCopilotXException(ResponseCode.KNOWLEDGE_LIB_NOT_FOUND);
        }
        ModelConfigBean modelConfigBean = modelConfigMapper.selectById(knowledgeLibBean.getModelId());
        if (ObjectUtils.isEmpty(modelConfigBean)) {
            throw new DataCopilotXException(ResponseCode.MODEL_NOT_FOUND);
        }

        List<Float> embedding = aiGatewayChatService.embedding(
                ChatRequest
                        .builder()
                        .apiKey(modelConfigBean.getApiKey())
                        .dimensions(modelConfigBean.getDimension())
                        .baseUrl(modelConfigBean.getBaseUrl())
                        .model(modelConfigBean.getModel())
                        .type(modelConfigBean.getType())
                        .question(retrievalForm.getQuestion())
                        .build()
        );
        if (ObjectUtils.isEmpty(embedding)) {
            throw new DataCopilotXException(ResponseCode.VECTOR_EMBEDDING_FAILED);
        }

        return elasticSearchVectorStorage.hybridRetrieval(knowledgeLibBean.getName(), embedding, retrievalForm.getQuestion(), retrievalForm.getTopK(), retrievalForm.getScore());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(int id) {
        try {
            KnowledgeLibBean knowledgeLibBean = knowledgeLibMapper.selectOne(new LambdaQueryWrapper<KnowledgeLibBean>().eq(KnowledgeLibBean::getId, id));
            knowledgeLibMapper.deleteById(id);
            elasticSearchVectorStorage.deleteIndex(knowledgeLibBean.getName());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new DataCopilotXException(ResponseCode.COMPONENT_FAILED);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void dataDelete(KnowledgeLibForm.DeleteForm deleteForm) {
        Long knowledgeLibId = deleteForm.getKnowledgeLibId();
        String docId = deleteForm.getDocId();
        KnowledgeLibBean knowledgeLibBean = knowledgeLibMapper.selectById(knowledgeLibId);
        if (ObjectUtils.isEmpty(knowledgeLibBean)) {
            throw new DataCopilotXException(ResponseCode.KNOWLEDGE_LIB_NOT_FOUND);
        }

        try {
            elasticSearchVectorStorage.deleteData(knowledgeLibBean.getName(), docId);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new DataCopilotXException(ResponseCode.COMPONENT_FAILED);
        }
    }

    public PageVO<List<ElasticSearchVectorData>> dataList(KnowledgeLibForm.PageForm pageForm) {
        KnowledgeLibBean knowledgeLibBean = knowledgeLibMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeLibBean>()
                        .eq(KnowledgeLibBean::getId, pageForm.getKnowledgeLibId())
        );
        if (ObjectUtils.isEmpty(knowledgeLibBean)) {
            throw new DataCopilotXException(ResponseCode.KNOWLEDGE_LIB_NOT_FOUND);
        }

        ElasticSearchVectorData.ElasticSearchFormDTO elasticSearchFormDTO = ElasticSearchVectorData.ElasticSearchFormDTO
                .builder()
                .enable(pageForm.getEnable())
                .name(pageForm.getName())
                .pageNo(pageForm.getPageNo())
                .pageSize(pageForm.getPageSize())
                .indexName(knowledgeLibBean.getName())
                .build();
        ElasticSearchVectorData.ElasticSearchVectorPageDTO elasticSearchVectorPageDTO = elasticSearchVectorStorage.pageIndexData(elasticSearchFormDTO);
        PageVO<List<ElasticSearchVectorData>> result = new PageVO<>();
        result.setData(elasticSearchVectorPageDTO.getData());
        result.setTotal(elasticSearchVectorPageDTO.getTotal());
        result.setPageNo(pageForm.getPageNo());
        result.setPageNo(pageForm.getPageSize());

        return result;
    }
}
