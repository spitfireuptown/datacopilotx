package com.datacopilotx.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacopilotx.ai.controller.form.KnowledgeLibForm;
import com.datacopilotx.ai.domian.dto.QueryDTO;
import com.datacopilotx.ai.domian.vo.KnowledgeLibVO;
import com.datacopilotx.ai.domian.vo.PageVO;
import com.datacopilotx.ai.service.KnowledgeLibService;
import com.datacopilotx.aigateway.domain.dto.ElasticSearchVectorData;
import com.datacopilotx.aigateway.domain.dto.OllamaResultDTO;
import com.datacopilotx.common.result.WebResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/knowledgelib")
public class KnowledgeLibController {

    @Autowired
    KnowledgeLibService knowledgeLibService;

    @PostMapping("/create")
    public WebResult<Long> create(@RequestBody KnowledgeLibForm.CreateForm createForm) {
        return WebResult.success(knowledgeLibService.create(createForm));
    }

    @PostMapping("/update")
    public WebResult<Long> update(@RequestBody KnowledgeLibForm.UpdateForm updateForm) {
        return WebResult.success(knowledgeLibService.update(updateForm));
    }

    @GetMapping("/list")
    public WebResult<List<KnowledgeLibVO.List>> list() {
        return WebResult.success(knowledgeLibService.list());
    }

    @GetMapping("/data/list")
    public WebResult<PageVO<List<ElasticSearchVectorData>>> dataList(KnowledgeLibForm.PageForm pageForm) {
        return WebResult.success(knowledgeLibService.dataList(pageForm));
    }

    @PostMapping("/data/store")
    public void store(@RequestBody KnowledgeLibForm.StoreForm storeForm) {
        knowledgeLibService.store(storeForm);
    }

    @PostMapping("/data/modify")
    public void modify(@RequestBody KnowledgeLibForm.UpdateDateForm updateDateForm) {
        knowledgeLibService.modify(updateDateForm);
    }

    @PostMapping("/data/delete")
    public void dataDelete(@RequestBody KnowledgeLibForm.DeleteForm deleteForm) {
        knowledgeLibService.dataDelete(deleteForm);
    }


    @PostMapping("/retrieval")
    public WebResult<List<OllamaResultDTO.CallBackResult>> retrieval(@RequestBody KnowledgeLibForm.RetrievalForm retrievalForm) {
        return WebResult.success(knowledgeLibService.retrieval(retrievalForm));
    }

    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable int id) {
        knowledgeLibService.delete(id);
    }
}
