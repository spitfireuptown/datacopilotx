package com.datacopilotx.ai.service.flow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datacopilotx.ai.controller.form.KnowledgeLibForm;
import com.datacopilotx.ai.controller.form.QuestionForm;
import com.datacopilotx.ai.domian.bean.KnowledgeLibBean;
import com.datacopilotx.ai.mapper.KnowledgeLibMapper;
import com.datacopilotx.ai.service.KnowledgeLibService;
import com.datacopilotx.aigateway.domain.dto.OllamaResultDTO;
import com.datacopilotx.common.result.WebResult;
import lombok.extern.slf4j.Slf4j;
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
public class RecallKnowledgeStep extends AbstractChatProcessStep {

    @Autowired
    KnowledgeLibService knowledgeLibService;
    @Autowired
    KnowledgeLibMapper knowledgeLibMapper;


    @Override
    public void process(Sinks.Many<ServerSentEvent<WebResult<String>>> sink, Map<String, Object> preResultMap, QuestionForm questionForm) {
        List<KnowledgeLibBean> knowledgeLibBeans = knowledgeLibMapper.selectList(
                new LambdaQueryWrapper<KnowledgeLibBean>()
                        .eq(KnowledgeLibBean::getDatasetId, questionForm.getDatasetId())
        );

        List<String> result = new ArrayList<>();
        String analysis = (String) preResultMap.get("analysis");
        KnowledgeLibForm.RetrievalForm retrievalForm = new KnowledgeLibForm.RetrievalForm();
        retrievalForm.setQuestion(analysis);
        retrievalForm.setScore(0.7F);
        retrievalForm.setTopK(5);
        for (KnowledgeLibBean knowledgeLibBean : knowledgeLibBeans) {
            retrievalForm.setKnowledgeLibId(knowledgeLibBean.getId());
            List<OllamaResultDTO.CallBackResult> results = knowledgeLibService.retrieval(retrievalForm);
            result.addAll(results.stream().map(OllamaResultDTO.CallBackResult::getAnswer).collect(Collectors.toList()));
        }
        Map<String, Object> currResultMap = Map.of("recall", result);
        proceedToNextStep(sink, currResultMap, questionForm);
    }
}
