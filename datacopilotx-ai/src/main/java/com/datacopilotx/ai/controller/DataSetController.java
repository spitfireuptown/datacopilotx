package com.datacopilotx.ai.controller;

import com.datacopilotx.ai.controller.form.DataSetForm;
import com.datacopilotx.ai.domian.dto.DataSetDTO;
import com.datacopilotx.ai.domian.vo.DataSetVO;
import com.datacopilotx.ai.service.DataSetService;
import com.datacopilotx.common.result.WebResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/dataset")
public class DataSetController {

    @Autowired
    private DataSetService dataSetService;

    @GetMapping("/list")
    public WebResult<List<DataSetVO.ListVO>> list() {
        return WebResult.success(dataSetService.list());
    }

    @PostMapping("/create")
    public WebResult<Long> create(@RequestBody DataSetForm.Create createForm) {
        return WebResult.success(dataSetService.create(createForm));
    }

    @PostMapping("/update")
    public WebResult<Long> update(@RequestBody DataSetForm.Create updateForm) {
        return WebResult.success(dataSetService.update(updateForm));
    }

    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable long id) {
        dataSetService.del(id);
    }

    @GetMapping("/detail/{id}")
    public WebResult<DataSetVO.DetailVO> detail(@PathVariable long id) {
        return WebResult.success(dataSetService.detail(id));
    }

    @RequestMapping("/table/info")
    public WebResult<List<DataSetDTO.SchemaInfo>> tableSchemaInfo(@RequestBody DataSetForm.Create createForm) {
        return WebResult.success(dataSetService.tableSchemaInfo(createForm));
    }

    @RequestMapping("/file/upload")
    public WebResult<Boolean> fileUpload(@RequestParam("file") MultipartFile file, String name, String description) {
        return WebResult.success();
    }
}
