package com.datacopilotx.ai.domian.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RulesDTO {

    private Long id;

    private Integer enable;

    private String name;

    private List<Long> permissionList;

    private List<String> userList;

    private List<PermissionDTO> permissions;
}