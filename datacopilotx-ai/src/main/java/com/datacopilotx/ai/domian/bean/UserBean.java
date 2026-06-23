package com.datacopilotx.ai.domian.bean;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
@TableName("SYSTEM_USER")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private String userId;

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    @TableField("nickname")
    private String nickname;

    @TableField("email")
    private String email;

    @TableField("phone")
    private String phone;

    @TableField("role")
    private Integer role;

    @TableField("status")
    private Integer status;

    @TableField("is_del")
    @TableLogic
    private Integer isDel;

    @TableField("ctime")
    private Timestamp ctime;

    @TableField("utime")
    private Timestamp utime;

    public enum Role {
        SUPER_ADMIN(0, "超级管理员"),
        ADMIN(1, "管理员"),
        USER(2, "普通用户");

        private final Integer code;
        private final String desc;

        Role(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public Integer getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }

        public static Role fromCode(Integer code) {
            for (Role role : values()) {
                if (role.code.equals(code)) {
                    return role;
                }
            }
            return USER;
        }
    }

    public enum Status {
        DISABLED(0, "禁用"),
        ENABLED(1, "启用");

        private final Integer code;
        private final String desc;

        Status(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public Integer getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }

        public static Status fromCode(Integer code) {
            for (Status status : values()) {
                if (status.code.equals(code)) {
                    return status;
                }
            }
            return DISABLED;
        }
    }
}