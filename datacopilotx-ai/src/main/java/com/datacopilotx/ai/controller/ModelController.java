package com.datacopilotx.ai.controller;

import com.datacopilotx.ai.controller.form.ModelForm;
import com.datacopilotx.ai.domian.vo.ModelConfigVO;
import com.datacopilotx.ai.service.ModelConfigService;
import com.datacopilotx.common.result.WebResult;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/model")
public class ModelController {

    @Resource
    ModelConfigService modelConfigService;


    @GetMapping("/list")
    public WebResult<List<ModelConfigVO.List>> list(String name, String type) {
        return WebResult.success(modelConfigService.list(name, type));
    }

    @PostMapping("/create")
    public WebResult create(@RequestBody ModelForm.Create createForm) {
        return WebResult.success(modelConfigService.create(createForm));
    }

    @PostMapping("/test-connection/{id}")
    public void testConnection(@PathVariable long id) {
        modelConfigService.testConnection(id);
    }

    @GetMapping("/modify")
    public WebResult modify(@RequestBody ModelForm.Create modifyForm) {
        return WebResult.success(modelConfigService.modify(modifyForm));
    }

    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable int id) {
        modelConfigService.del(id);
    }
}
