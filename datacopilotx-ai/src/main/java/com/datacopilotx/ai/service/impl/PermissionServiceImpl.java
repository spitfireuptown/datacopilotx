package com.datacopilotx.ai.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datacopilotx.ai.domian.bean.DataSetBean;
import com.datacopilotx.ai.domian.bean.DsPermission;
import com.datacopilotx.ai.domian.bean.DsRules;
import com.datacopilotx.ai.domian.dto.PermissionDTO;
import com.datacopilotx.ai.domian.dto.RulesDTO;
import com.datacopilotx.ai.mapper.DataSetMapper;
import com.datacopilotx.ai.mapper.DsPermissionMapper;
import com.datacopilotx.ai.mapper.DsRulesMapper;
import com.datacopilotx.ai.service.PermissionService;
import com.datacopilotx.ai.util.RowPermissionUtil;
import com.datacopilotx.ai.util.SecurityUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PermissionServiceImpl implements PermissionService {

    @Resource
    private DsPermissionMapper dsPermissionMapper;

    @Resource
    private DsRulesMapper dsRulesMapper;

    @Resource
    private DataSetMapper dataSetMapper;

    @Override
    public PermissionDTO createPermission(PermissionDTO permissionDTO) {
        DsPermission permission = convertToBean(permissionDTO);
        permission.setCreator(SecurityUtil.getCurrentUserId());
        permission.setEnable(1);
        permission.setIsDel(0);
        dsPermissionMapper.insert(permission);
        return convertToDTO(permission);
    }

    @Override
    public PermissionDTO updatePermission(PermissionDTO permissionDTO) {
        DsPermission permission = dsPermissionMapper.selectById(permissionDTO.getId());
        if (permission == null) {
            throw new IllegalArgumentException("权限规则不存在");
        }
        permission.setName(permissionDTO.getName());
        permission.setEnable(permissionDTO.getEnable());
        permission.setType(permissionDTO.getType());
        permission.setDsId(permissionDTO.getDsId());
        permission.setTableId(permissionDTO.getTableId());
        permission.setTableName(permissionDTO.getTableName());

        if (permissionDTO.getExpressionTree() != null) {
            permission.setExpressionTree(JSONUtil.toJsonStr(permissionDTO.getExpressionTree()));
        }
        if (permissionDTO.getPermissions() != null) {
            permission.setPermissions(JSONUtil.toJsonStr(permissionDTO.getPermissions()));
        }
        if (permissionDTO.getWhiteListUser() != null) {
            permission.setWhiteListUser(JSONUtil.toJsonStr(permissionDTO.getWhiteListUser()));
        }

        dsPermissionMapper.updateById(permission);
        return convertToDTO(permission);
    }

    @Override
    public void deletePermission(Long id) {
        dsPermissionMapper.deleteById(id);
    }

    @Override
    public PermissionDTO getPermissionById(Long id) {
        DsPermission permission = dsPermissionMapper.selectById(id);
        return permission != null ? convertToDTO(permission) : null;
    }

    @Override
    public List<PermissionDTO> getPermissionsByDsId(Long dsId) {
        List<DsPermission> permissions = dsPermissionMapper.selectList(new LambdaQueryWrapper<DsPermission>()
                .eq(DsPermission::getDsId, dsId)
                .eq(DsPermission::getEnable, 1));
        return permissions.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<PermissionDTO> getRowPermissionsByDsId(Long dsId) {
        List<DsPermission> permissions = dsPermissionMapper.selectList(new LambdaQueryWrapper<DsPermission>()
                .eq(DsPermission::getDsId, dsId)
                .eq(DsPermission::getType, DsPermission.PermissionType.ROW.getCode())
                .eq(DsPermission::getEnable, 1));
        return permissions.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<PermissionDTO> getColumnPermissionsByDsId(Long dsId) {
        List<DsPermission> permissions = dsPermissionMapper.selectList(new LambdaQueryWrapper<DsPermission>()
                .eq(DsPermission::getDsId, dsId)
                .eq(DsPermission::getType, DsPermission.PermissionType.COLUMN.getCode())
                .eq(DsPermission::getEnable, 1));
        return permissions.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<PermissionDTO> batchCreatePermissions(List<PermissionDTO> permissionDTOList) {
        List<PermissionDTO> results = new ArrayList<>();
        for (PermissionDTO dto : permissionDTOList) {
            results.add(createPermission(dto));
        }
        return results;
    }

    @Override
    public RulesDTO createRule(RulesDTO rulesDTO) {
        DsRules rule = convertRuleToBean(rulesDTO);
        rule.setCreator(SecurityUtil.getCurrentUserId());
        rule.setEnable(1);
        rule.setIsDel(0);
        dsRulesMapper.insert(rule);
        return convertRuleToDTO(rule);
    }

    @Override
    public RulesDTO updateRule(RulesDTO rulesDTO) {
        DsRules rule = dsRulesMapper.selectById(rulesDTO.getId());
        if (rule == null) {
            throw new IllegalArgumentException("规则组不存在");
        }
        rule.setName(rulesDTO.getName());
        rule.setEnable(rulesDTO.getEnable());
        rule.setPermissionList(JSONUtil.toJsonStr(rulesDTO.getPermissionList()));
        rule.setUserList(JSONUtil.toJsonStr(rulesDTO.getUserList()));
        dsRulesMapper.updateById(rule);
        return convertRuleToDTO(rule);
    }

    @Override
    public void deleteRule(Long id) {
        dsRulesMapper.deleteById(id);
    }

    @Override
    public RulesDTO getRuleById(Long id) {
        DsRules rule = dsRulesMapper.selectById(id);
        return rule != null ? convertRuleToDTO(rule) : null;
    }

    @Override
    public List<RulesDTO> getAllRules() {
        List<DsRules> rules = dsRulesMapper.selectList(new LambdaQueryWrapper<DsRules>()
                .eq(DsRules::getEnable, 1));
        return rules.stream().map(this::convertRuleToDTO).collect(Collectors.toList());
    }

    @Override
    public String getRowPermissionFilter(Long dsId, String userId, Map<String, Object> userInfo) {
        return getRowPermissionFilter(dsId, null, userId, userInfo);
    }

    @Override
    public String getRowPermissionFilter(Long dsId, Long tableId, String userId, Map<String, Object> userInfo) {
        if (SecurityUtil.isAdmin()) {
            return "";
        }

        List<PermissionDTO> rowPermissions = getRowPermissionsByDsIdAndTableId(dsId, tableId);
        if (CollectionUtils.isEmpty(rowPermissions)) {
            return "";
        }

        List<String> filters = new ArrayList<>();
        for (PermissionDTO permission : rowPermissions) {
            if (isUserInRule(userId, permission.getId())) {
                String sql = RowPermissionUtil.convertExpressionTreeToSql(
                        JSONUtil.toJsonStr(permission.getExpressionTree()),
                        userId,
                        userInfo
                );
                if (!sql.isEmpty()) {
                    filters.add(sql);
                }
            }
        }

        return filters.isEmpty() ? "" : String.join(" AND ", filters);
    }

    @Override
    public List<String> getColumnPermissionFields(Long dsId, String userId, List<String> allFields) {
        return getColumnPermissionFields(dsId, null, userId, allFields);
    }

    @Override
    public List<String> getColumnPermissionFields(Long dsId, Long tableId, String userId, List<String> allFields) {
        if (SecurityUtil.isAdmin()) {
            return allFields;
        }

        List<PermissionDTO> columnPermissions = getColumnPermissionsByDsIdAndTableId(dsId, tableId);
        if (CollectionUtils.isEmpty(columnPermissions)) {
            return allFields;
        }

        List<String> allowedFields = new ArrayList<>(allFields);
        for (PermissionDTO permission : columnPermissions) {
            if (isUserInRule(userId, permission.getId())) {
                if (permission.getPermissions() != null) {
                    for (PermissionDTO.ColumnPermission cp : permission.getPermissions()) {
                        if (!cp.getEnable()) {
                            allowedFields.remove(cp.getFieldName());
                        }
                    }
                }
            }
        }

        return allowedFields;
    }

    private List<PermissionDTO> getRowPermissionsByDsIdAndTableId(Long dsId, Long tableId) {
        LambdaQueryWrapper<DsPermission> queryWrapper = new LambdaQueryWrapper<DsPermission>()
                .eq(DsPermission::getDsId, dsId)
                .eq(DsPermission::getType, DsPermission.PermissionType.ROW.getCode())
                .eq(DsPermission::getEnable, 1);
        if (tableId != null) {
            queryWrapper.and(w -> w.eq(DsPermission::getTableId, tableId).or().isNull(DsPermission::getTableId));
        }
        List<DsPermission> permissions = dsPermissionMapper.selectList(queryWrapper);
        return permissions.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private List<PermissionDTO> getColumnPermissionsByDsIdAndTableId(Long dsId, Long tableId) {
        LambdaQueryWrapper<DsPermission> queryWrapper = new LambdaQueryWrapper<DsPermission>()
                .eq(DsPermission::getDsId, dsId)
                .eq(DsPermission::getType, DsPermission.PermissionType.COLUMN.getCode())
                .eq(DsPermission::getEnable, 1);
        if (tableId != null) {
            queryWrapper.and(w -> w.eq(DsPermission::getTableId, tableId).or().isNull(DsPermission::getTableId));
        }
        List<DsPermission> permissions = dsPermissionMapper.selectList(queryWrapper);
        return permissions.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public boolean isUserInRule(String userId, Long permissionId) {
        List<DsRules> rules = dsRulesMapper.selectList(new LambdaQueryWrapper<DsRules>()
                .eq(DsRules::getEnable, 1));

        for (DsRules rule : rules) {
            List<Long> permissionList = JSONUtil.toList(rule.getPermissionList(), Long.class);
            List<String> userList = JSONUtil.toList(rule.getUserList(), String.class);

            if (permissionList.contains(permissionId) && userList.contains(userId)) {
                return true;
            }
        }

        return false;
    }

    private DsPermission convertToBean(PermissionDTO dto) {
        return DsPermission.builder()
                .id(dto.getId())
                .enable(dto.getEnable())
                .type(dto.getType())
                .dsId(dto.getDsId())
                .tableId(dto.getTableId())
                .tableName(dto.getTableName())
                .name(dto.getName())
                .expressionTree(dto.getExpressionTree() != null ? JSONUtil.toJsonStr(dto.getExpressionTree()) : null)
                .permissions(dto.getPermissions() != null ? JSONUtil.toJsonStr(dto.getPermissions()) : null)
                .whiteListUser(dto.getWhiteListUser() != null ? JSONUtil.toJsonStr(dto.getWhiteListUser()) : null)
                .creator(SecurityUtil.getCurrentUserId())
                .build();
    }

    private PermissionDTO convertToDTO(DsPermission bean) {
        PermissionDTO.PermissionDTOBuilder builder = PermissionDTO.builder()
                .id(bean.getId())
                .enable(bean.getEnable())
                .type(bean.getType())
                .dsId(bean.getDsId())
                .tableId(bean.getTableId())
                .tableName(bean.getTableName())
                .name(bean.getName());

        if (bean.getDsId() != null) {
            DataSetBean dataSet = dataSetMapper.selectById(bean.getDsId());
            if (dataSet != null) {
                builder.dsName(dataSet.getDsName());
            }
        }

        if (bean.getExpressionTree() != null) {
            builder.expressionTree(JSONUtil.toBean(bean.getExpressionTree(), PermissionDTO.ExpressionTree.class));
        }
        if (bean.getPermissions() != null) {
            builder.permissions(JSONUtil.toList(bean.getPermissions(), PermissionDTO.ColumnPermission.class));
        }
        if (bean.getWhiteListUser() != null) {
            builder.whiteListUser(JSONUtil.toList(bean.getWhiteListUser(), String.class));
        }

        return builder.build();
    }

    private DsRules convertRuleToBean(RulesDTO dto) {
        return DsRules.builder()
                .id(dto.getId())
                .enable(dto.getEnable())
                .name(dto.getName())
                .permissionList(JSONUtil.toJsonStr(dto.getPermissionList()))
                .userList(JSONUtil.toJsonStr(dto.getUserList()))
                .build();
    }

    private RulesDTO convertRuleToDTO(DsRules bean) {
        List<Long> permissionList = JSONUtil.toList(bean.getPermissionList(), Long.class);
        List<PermissionDTO> permissions = new ArrayList<>();
        for (Long permId : permissionList) {
            DsPermission permission = dsPermissionMapper.selectById(permId);
            if (permission != null) {
                permissions.add(convertToDTO(permission));
            }
        }
        return RulesDTO.builder()
                .id(bean.getId())
                .enable(bean.getEnable())
                .name(bean.getName())
                .permissionList(permissionList)
                .userList(JSONUtil.toList(bean.getUserList(), String.class))
                .permissions(permissions)
                .build();
    }
}