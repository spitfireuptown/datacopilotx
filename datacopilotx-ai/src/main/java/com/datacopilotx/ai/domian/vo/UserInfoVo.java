package com.datacopilotx.ai.domian.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoVo {
    private String userId;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private Integer role;
    private String roleDesc;
    private Integer status;
    private String statusDesc;
    private Long createdAt;
    private Long updatedAt;
}