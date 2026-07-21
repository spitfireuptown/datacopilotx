package com.datacopilotx.ai.domian.bean;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
@TableName("DS_PERMISSION")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DsPermission implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("enable")
    private Integer enable;

    @TableField("type")
    private String type;

    @TableField("ds_id")
    private Long dsId;

    @TableField("table_id")
    private Long tableId;

    @TableField("table_name")
    private String tableName;

    @TableField("name")
    private String name;

    @TableField("expression_tree")
    private String expressionTree;

    @TableField("permissions")
    private String permissions;

    @TableField("white_list_user")
    private String whiteListUser;

    @TableField("creator")
    private String creator;

    @TableField("is_del")
    @TableLogic
    private Integer isDel;

    @TableField("ctime")
    private Timestamp ctime;

    @TableField("utime")
    private Timestamp utime;

    public enum PermissionType {
        ROW("row", "行权限"),
        COLUMN("column", "列权限");

        private final String code;
        private final String desc;

        PermissionType(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }

        public static PermissionType fromCode(String code) {
            for (PermissionType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return ROW;
        }
    }
}