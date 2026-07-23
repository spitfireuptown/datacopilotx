package com.datacopilotx.ai.service.graph;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datacopilotx.ai.controller.form.KnowledgeLibForm;
import com.datacopilotx.ai.domian.bean.KnowledgeLibBean;
import com.datacopilotx.ai.mapper.KnowledgeLibMapper;
import com.datacopilotx.ai.service.KnowledgeLibService;
import com.datacopilotx.ai.service.graph.main.WorkflowServiceHelper;
import com.datacopilotx.ai.service.graph.main.WorkflowState;
import com.datacopilotx.aigateway.domain.dto.OllamaResultDTO;
import com.datacopilotx.common.constant.PromptConstant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RecallKnowledgeGraphNode implements NodeAction<WorkflowState> {

    @Resource
    KnowledgeLibService knowledgeLibService;
    @Resource
    KnowledgeLibMapper knowledgeLibMapper;
    @Resource
    WorkflowServiceHelper workflowServiceHelper;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        Long datasetId = state.datasetId().orElseThrow(() -> new IllegalArgumentException("datasetId is empty"));
        String question = state.question().orElseThrow(() -> new IllegalArgumentException("question is empty"));

        List<KnowledgeLibBean> knowledgeLibBeans = knowledgeLibMapper.selectList(
                new LambdaQueryWrapper<KnowledgeLibBean>()
                        .eq(KnowledgeLibBean::getDatasetId, datasetId)
        );

        var sink = state.getSink();
        var serializableSink = state.getSerializableSink();

        workflowServiceHelper.streamPrint(sink, PromptConstant.RECALL_NODE, "\n", serializableSink, state);
        workflowServiceHelper.streamPrint(sink, PromptConstant.RECALL_NODE, "#### 知识库匹配: ", serializableSink, state);
        workflowServiceHelper.streamPrint(sink, PromptConstant.RECALL_NODE, "\n", serializableSink, state);
        log.info("Found {} knowledge libraries for dataset {}", knowledgeLibBeans.size(), datasetId);

        List<String> result = new ArrayList<>();
        KnowledgeLibForm.RetrievalForm retrievalForm = new KnowledgeLibForm.RetrievalForm();
        retrievalForm.setQuestion(question);
        retrievalForm.setTopK(5);

        for (KnowledgeLibBean knowledgeLibBean : knowledgeLibBeans) {
            retrievalForm.setKnowledgeLibId(knowledgeLibBean.getId());
            retrievalForm.setScore(knowledgeLibBean.getScore() != null ? knowledgeLibBean.getScore() : 0.7F);
            List<OllamaResultDTO.CallBackResult> results = knowledgeLibService.retrieval(retrievalForm);
            result.addAll(results.stream().map(OllamaResultDTO.CallBackResult::getAnswer).collect(Collectors.toList()));
        }
        workflowServiceHelper.streamPrint(sink, PromptConstant.RECALL_NODE, String.format("已匹配%s条知识: ", knowledgeLibBeans.size()), serializableSink, state);

        log.info("Knowledge retrieval completed, found {} relevant results", result.size());
        Map<String, Object> returnMap = new HashMap<>();
        returnMap.put("recall", result);
        returnMap.putAll(state.appendCollectedData("\n#### 知识库匹配: \n" + String.format("已匹配%s条知识", knowledgeLibBeans.size())));
        return returnMap;
    }
}