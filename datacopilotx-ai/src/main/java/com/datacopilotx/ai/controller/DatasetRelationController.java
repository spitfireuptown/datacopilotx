package com.datacopilotx.ai.controller;

import com.datacopilotx.ai.controller.form.DatasetRelationForm;
import com.datacopilotx.ai.domian.vo.DatasetRelationVO;
import com.datacopilotx.ai.service.DatasetRelationService;
import com.datacopilotx.common.result.WebResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dataset/relation")
public class DatasetRelationController {

    @Autowired
    private DatasetRelationService datasetRelationService;

    @GetMapping("/list")
    public WebResult<List<DatasetRelationVO.ListVO>> list() {
        return WebResult.success(datasetRelationService.list());
    }

    @PostMapping("/create")
    public WebResult<Long> create(@RequestBody DatasetRelationForm.Create createForm) {
        return WebResult.success(datasetRelationService.create(createForm));
    }

    @PostMapping("/update")
    public WebResult<Long> update(@RequestBody DatasetRelationForm.Create updateForm) {
        return WebResult.success(datasetRelationService.update(updateForm));
    }

    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable long id) {
        datasetRelationService.del(id);
    }

    @GetMapping("/detail/{id}")
    public WebResult<DatasetRelationVO.DetailVO> detail(@PathVariable long id) {
        return WebResult.success(datasetRelationService.detail(id));
    }

    @GetMapping("/list/{datasetId}")
    public WebResult<List<DatasetRelationVO.ListVO>> listByDatasetId(@PathVariable long datasetId) {
        return WebResult.success(datasetRelationService.listByDatasetId(datasetId));
    }
}