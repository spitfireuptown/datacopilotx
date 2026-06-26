package com.datacopilotx.ai.service;

import com.datacopilotx.ai.domian.dto.PermissionDTO;
import com.datacopilotx.ai.domian.dto.RulesDTO;

import java.util.List;
import java.util.Map;

public interface PermissionService {

    PermissionDTO createPermission(PermissionDTO permissionDTO);

    PermissionDTO updatePermission(PermissionDTO permissionDTO);

    void deletePermission(Long id);

    PermissionDTO getPermissionById(Long id);

    List<PermissionDTO> getPermissionsByDsId(Long dsId);

    List<PermissionDTO> getRowPermissionsByDsId(Long dsId);

    List<PermissionDTO> getColumnPermissionsByDsId(Long dsId);

    List<PermissionDTO> batchCreatePermissions(List<PermissionDTO> permissionDTOList);

    RulesDTO createRule(RulesDTO rulesDTO);

    RulesDTO updateRule(RulesDTO rulesDTO);

    void deleteRule(Long id);

    RulesDTO getRuleById(Long id);

    List<RulesDTO> getAllRules();

    String getRowPermissionFilter(Long dsId, String userId, Map<String, Object> userInfo);

    List<String> getColumnPermissionFields(Long dsId, String userId, List<String> allFields);

    boolean isUserInRule(String userId, Long permissionId);
}