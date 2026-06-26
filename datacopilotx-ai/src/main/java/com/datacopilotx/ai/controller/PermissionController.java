package com.datacopilotx.ai.controller;

import com.datacopilotx.ai.controller.form.PermissionForm;
import com.datacopilotx.ai.controller.form.RuleForm;
import com.datacopilotx.ai.domian.dto.PermissionDTO;
import com.datacopilotx.ai.domian.dto.RulesDTO;
import com.datacopilotx.ai.service.PermissionService;
import com.datacopilotx.common.result.WebResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/permission")
public class PermissionController {

    @Resource
    private PermissionService permissionService;

    @PostMapping("/create")
    public WebResult<PermissionDTO> createPermission(@RequestBody PermissionForm form) {
        PermissionDTO dto = convertToDTO(form);
        PermissionDTO result = permissionService.createPermission(dto);
        return WebResult.success(result);
    }

    @PutMapping("/update")
    public WebResult<PermissionDTO> updatePermission(@RequestBody PermissionForm form) {
        PermissionDTO dto = convertToDTO(form);
        PermissionDTO result = permissionService.updatePermission(dto);
        return WebResult.success(result);
    }

    @DeleteMapping("/delete/{id}")
    public WebResult<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return WebResult.success();
    }

    @GetMapping("/get/{id}")
    public WebResult<PermissionDTO> getPermissionById(@PathVariable Long id) {
        PermissionDTO result = permissionService.getPermissionById(id);
        return WebResult.success(result);
    }

    @GetMapping("/list/{dsId}")
    public WebResult<List<PermissionDTO>> getPermissionsByDsId(@PathVariable Long dsId) {
        List<PermissionDTO> result = permissionService.getPermissionsByDsId(dsId);
        return WebResult.success(result);
    }

    @GetMapping("/row/{dsId}")
    public WebResult<List<PermissionDTO>> getRowPermissionsByDsId(@PathVariable Long dsId) {
        List<PermissionDTO> result = permissionService.getRowPermissionsByDsId(dsId);
        return WebResult.success(result);
    }

    @GetMapping("/column/{dsId}")
    public WebResult<List<PermissionDTO>> getColumnPermissionsByDsId(@PathVariable Long dsId) {
        List<PermissionDTO> result = permissionService.getColumnPermissionsByDsId(dsId);
        return WebResult.success(result);
    }

    @PostMapping("/batch")
    public WebResult<List<PermissionDTO>> batchCreatePermissions(@RequestBody List<PermissionForm> forms) {
        List<PermissionDTO> dtoList = forms.stream().map(this::convertToDTO).collect(Collectors.toList());
        List<PermissionDTO> result = permissionService.batchCreatePermissions(dtoList);
        return WebResult.success(result);
    }

    @PostMapping("/rule/create")
    public WebResult<RulesDTO> createRule(@RequestBody RuleForm form) {
        RulesDTO dto = convertRuleToDTO(form);
        RulesDTO result = permissionService.createRule(dto);
        return WebResult.success(result);
    }

    @PutMapping("/rule/update")
    public WebResult<RulesDTO> updateRule(@RequestBody RuleForm form) {
        RulesDTO dto = convertRuleToDTO(form);
        RulesDTO result = permissionService.updateRule(dto);
        return WebResult.success(result);
    }

    @DeleteMapping("/rule/delete/{id}")
    public WebResult<Void> deleteRule(@PathVariable Long id) {
        permissionService.deleteRule(id);
        return WebResult.success();
    }

    @GetMapping("/rule/get/{id}")
    public WebResult<RulesDTO> getRuleById(@PathVariable Long id) {
        RulesDTO result = permissionService.getRuleById(id);
        return WebResult.success(result);
    }

    @GetMapping("/rule/list")
    public WebResult<List<RulesDTO>> getAllRules() {
        List<RulesDTO> result = permissionService.getAllRules();
        return WebResult.success(result);
    }

    private PermissionDTO convertToDTO(PermissionForm form) {
        PermissionDTO.PermissionDTOBuilder builder = PermissionDTO.builder()
                .id(form.getId())
                .enable(form.getEnable())
                .type(form.getType())
                .dsId(form.getDsId())
                .name(form.getName())
                .whiteListUser(form.getWhiteListUser());

        if (form.getExpressionTree() != null) {
            builder.expressionTree(PermissionDTO.ExpressionTree.builder()
                    .logic(form.getExpressionTree().getLogic())
                    .items(form.getExpressionTree().getItems().stream()
                            .map(item -> PermissionDTO.ExpressionItem.builder()
                                    .type(item.getType())
                                    .fieldId(item.getFieldId())
                                    .fieldName(item.getFieldName())
                                    .filterType(item.getFilterType())
                                    .term(item.getTerm())
                                    .value(item.getValue())
                                    .subTree(item.getSubTree() != null ? PermissionDTO.ExpressionTree.builder()
                                            .logic(item.getSubTree().getLogic())
                                            .items(item.getSubTree().getItems().stream()
                                                    .map(subItem -> PermissionDTO.ExpressionItem.builder()
                                                            .type(subItem.getType())
                                                            .fieldId(subItem.getFieldId())
                                                            .fieldName(subItem.getFieldName())
                                                            .filterType(subItem.getFilterType())
                                                            .term(subItem.getTerm())
                                                            .value(subItem.getValue())
                                                            .build())
                                                    .toList())
                                            .build() : null)
                                    .build())
                            .toList())
                    .build());
        }

        if (form.getPermissions() != null) {
            builder.permissions(form.getPermissions().stream()
                    .map(cp -> PermissionDTO.ColumnPermission.builder()
                            .fieldId(cp.getFieldId())
                            .fieldName(cp.getFieldName())
                            .fieldComment(cp.getFieldComment())
                            .enable(cp.getEnable())
                            .build())
                    .toList());
        }

        return builder.build();
    }

    private RulesDTO convertRuleToDTO(RuleForm form) {
        return RulesDTO.builder()
                .id(form.getId())
                .enable(form.getEnable())
                .name(form.getName())
                .permissionList(form.getPermissionList())
                .userList(form.getUserList())
                .build();
    }
}