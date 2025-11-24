package com.datacopilotx.ai.service.graph;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datacopilotx.ai.controller.form.KnowledgeLibForm;
import com.datacopilotx.ai.controller.form.QuestionForm;
import com.datacopilotx.ai.domian.bean.KnowledgeLibBean;
import com.datacopilotx.ai.mapper.KnowledgeLibMapper;
import com.datacopilotx.ai.service.KnowledgeLibService;
import com.datacopilotx.ai.service.flow.AbstractChatProcessStep;
import com.datacopilotx.aigateway.domain.dto.OllamaResultDTO;
import com.datacopilotx.common.result.WebResult;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RecallKnowledgeGraphNode implements NodeAction<WorkflowState> {

    @Autowired
    KnowledgeLibService knowledgeLibService;
    @Autowired
    KnowledgeLibMapper knowledgeLibMapper;

    @Override
    public Map<String, Object> apply(WorkflowState workflowState) throws Exception {
        String datasetId = workflowState.datasetId().orElseThrow(() -> new IllegalArgumentException("datasetId is empty"));
        String analysis = workflowState.intentAnalysis().orElseThrow(() -> new IllegalArgumentException("intentAnalysis is empty"));

        List<KnowledgeLibBean> knowledgeLibBeans = knowledgeLibMapper.selectList(
                new LambdaQueryWrapper<KnowledgeLibBean>()
                        .eq(KnowledgeLibBean::getDatasetId, datasetId)
        );

        List<String> result = new ArrayList<>();
        KnowledgeLibForm.RetrievalForm retrievalForm = new KnowledgeLibForm.RetrievalForm();
        retrievalForm.setQuestion(analysis);
        retrievalForm.setScore(0.7F);
        retrievalForm.setTopK(5);
        for (KnowledgeLibBean knowledgeLibBean : knowledgeLibBeans) {
            retrievalForm.setKnowledgeLibId(knowledgeLibBean.getId());
            List<OllamaResultDTO.CallBackResult> results = knowledgeLibService.retrieval(retrievalForm);
            result.addAll(results.stream().map(OllamaResultDTO.CallBackResult::getAnswer).collect(Collectors.toList()));
        }
        return Map.of("recall", result);
    }
}
