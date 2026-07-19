package com.datacopilotx.ai.controller;

import com.datacopilotx.ai.domian.bean.DataTableBean;
import com.datacopilotx.ai.service.DataSetService;
import com.datacopilotx.ai.service.DataTableService;
import com.datacopilotx.common.result.WebResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dataset/table")
public class DataTableController {

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataSetService dataSetService;

    @GetMapping("/list/{datasetId}")
    public WebResult<List<DataTableBean>> listByDatasetId(@PathVariable Long datasetId) {
        return WebResult.success(dataTableService.listByDatasetId(datasetId));
    }

    @PostMapping("/create")
    public WebResult<Long> create(@RequestBody DataTableBean dataTableBean) {
        return WebResult.success(dataTableService.create(dataTableBean));
    }

    @PostMapping("/update")
    public WebResult<Long> update(@RequestBody DataTableBean dataTableBean) {
        return WebResult.success(dataTableService.update(dataTableBean));
    }

    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable Long id) {
        dataTableService.del(id);
    }

    @GetMapping("/detail/{id}")
    public WebResult<DataTableBean> detail(@PathVariable Long id) {
        return WebResult.success(dataTableService.detail(id));
    }
}