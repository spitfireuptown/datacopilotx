package com.datacopilotx.ai.controller.form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RuleForm {

    private Long id;

    private Integer enable;

    private String name;

    private List<Long> permissionList;

    private List<String> userList;
}