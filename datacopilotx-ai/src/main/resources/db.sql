CREATE database if NOT EXISTS `datacopilotx` default character set utf8mb4 collate utf8mb4_unicode_ci;
use `datacopilotx`;

SET NAMES utf8mb4;

CREATE TABLE `DATA_SET` (
    `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
    `host` char(255) DEFAULT NULL COMMENT 'host',
    `ds_name` char(255) DEFAULT NULL COMMENT '数据集名称',
    `password` char(255) DEFAULT NULL COMMENT '数据集密码',
    `port` int(11) NOT NULL DEFAULT '0' COMMENT '端口',
    `username` varchar(255) DEFAULT NULL COMMENT '数据集用户名',
    `database` char(255) DEFAULT NULL COMMENT '数据库',
    `type` varchar(255) NOT NULL DEFAULT '0' COMMENT '数据集类型',
    `creator` VARCHAR(36) NOT NULL UNIQUE COMMENT '用户ID',
    `ctime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `utime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_del` int(11) DEFAULT '0',
    `description` varchar(255) DEFAULT NULL COMMENT '数据集描述',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COMMENT='数据集信息';

CREATE TABLE `DATA_TABLE` (
    `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
    `dataset_id` int(10) unsigned NOT NULL COMMENT '数据集ID',
    `table` char(255) DEFAULT NULL COMMENT '数据表名',
    `fields` longtext CHARACTER SET utf8 COMMENT '表字段元数据（JSON）',
    `inject_prompt` longtext CHARACTER SET utf8 COMMENT '表注入prompt',
    `embedding` longtext CHARACTER SET utf8 COMMENT '表嵌入向量（JSON数组）',
    `ctime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `utime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_del` int(11) DEFAULT '0',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX idx_dataset_id (`dataset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据表信息';



CREATE TABLE `MODEL_CONFIG` (
    `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
    `model` char(255) CHARACTER SET utf8 DEFAULT NULL,
    `api_key` char(255) DEFAULT NULL COMMENT '模型api_key',
    `base_url` char(255) DEFAULT NULL COMMENT '模型交互地址',
    `platform` varchar(100) DEFAULT NULL,
    `config` text COMMENT '附加配置',
    `function_type` varchar(100) DEFAULT NULL COMMENT '模型功能类型，embedding、chat',
    `type` varchar(100) DEFAULT NULL COMMENT '模型类型',
    `creator` VARCHAR(36) NOT NULL UNIQUE COMMENT '用户ID',
    `is_del` int(11) NOT NULL DEFAULT '0',
    `ctime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `utime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `dimension` int(11) NOT NULL DEFAULT '0' COMMENT '维度',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COMMENT='模型配置信息';

CREATE TABLE `QUESTION_LOG` (
    `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
    `question_id` char(64) CHARACTER SET utf8 NOT NULL,
    `session_id` char(64) CHARACTER SET utf8 NOT NULL,
    `dataset_id` char(64) NOT NULL DEFAULT '' COMMENT '数据集id',
    `model_id` char(64) NOT NULL DEFAULT '' COMMENT '使用模型id',
    `creator` VARCHAR(36) NOT NULL UNIQUE COMMENT '用户ID',
    `question` char(255) NOT NULL DEFAULT '' COMMENT '问题',
    `cost_token` int(11) DEFAULT NULL COMMENT '消耗token数',
    `cost_time` char(255) DEFAULT NULL COMMENT '耗时',
    `answer` longtext COMMENT '回答',
    `result` longtext CHARACTER SET utf8 COMMENT '最终结果',
    `is_del` int(11) NOT NULL DEFAULT '0',
    `ctime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `utime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    KEY `question_key` (`question_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=343 DEFAULT CHARSET=utf8mb4 COMMENT='查询历史日志';

CREATE TABLE `KNOWLEDGE_LIB` (
     `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
     `name` char(255) CHARACTER SET utf8 DEFAULT NULL,
     `dataset_id` char(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
     `model_id` char(64) CHARACTER SET utf8 NOT NULL DEFAULT '',
     `creator` VARCHAR(36) NOT NULL UNIQUE COMMENT '用户ID',
     `description` varchar(255) DEFAULT NULL COMMENT '模型平台',
     `is_del` int(11) NOT NULL DEFAULT '0',
     `ctime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
     `utime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COMMENT='知识库配置';

CREATE TABLE DATA_SET_RELATION (
       id BIGINT AUTO_INCREMENT PRIMARY KEY,
       from_datasource_id BIGINT NOT NULL,
       creator VARCHAR(36) NOT NULL UNIQUE COMMENT '用户ID',
       from_field VARCHAR(255),
       to_datasource_id BIGINT NOT NULL,
       to_field VARCHAR(255),
       relation_type VARCHAR(50),
       description VARCHAR(500),
       `ctime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
       `utime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
       is_del INT DEFAULT 0
);

-- 创建用户表
CREATE TABLE IF NOT EXISTS SYSTEM_USER (
   id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
   user_id VARCHAR(36) NOT NULL UNIQUE COMMENT '用户ID',
    username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(256) NOT NULL COMMENT '密码',
    nickname VARCHAR(64) COMMENT '昵称',
    email VARCHAR(128) COMMENT '邮箱',
    phone VARCHAR(32) COMMENT '手机号',
    role INT NOT NULL DEFAULT 2 COMMENT '角色：0-超级管理员，1-管理员，2-普通用户',
    status INT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    is_del INT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    ctime TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    utime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_user_id (user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 插入默认超级管理员账号（用户名：admin，密码：datacopilotx）
-- 密码是BCrypt加密后的datacopilotx
INSERT INTO SYSTEM_USER (user_id, username, password, nickname, role, status, is_del)
VALUES ('admin-001', 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '超级管理员', 0, 1, 0)
    ON DUPLICATE KEY UPDATE user_id=user_id;

-- 数据权限表 - 定义行权限和列权限规则
CREATE TABLE IF NOT EXISTS `DS_PERMISSION` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `enable` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    `type` VARCHAR(64) NOT NULL COMMENT '权限类型：row（行权限）/ column（列权限）',
    `ds_id` BIGINT NOT NULL COMMENT '数据集ID',
    `name` VARCHAR(128) NOT NULL COMMENT '权限规则名称',
    `expression_tree` LONGTEXT COMMENT '行权限表达式树（JSON格式）',
    `permissions` LONGTEXT COMMENT '列权限字段列表（JSON格式）',
    `white_list_user` LONGTEXT COMMENT '白名单用户（JSON数组）',
    `creator` VARCHAR(36) NOT NULL COMMENT '创建人ID',
    `is_del` INT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    `ctime` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_ds_id (`ds_id`),
    INDEX idx_type (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据权限表';

-- 权限规则组表 - 将权限规则和用户关联
CREATE TABLE IF NOT EXISTS `DS_RULES` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `enable` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    `name` VARCHAR(128) NOT NULL COMMENT '规则组名称',
    `permission_list` LONGTEXT NOT NULL COMMENT '关联的权限ID列表（JSON数组）',
    `user_list` LONGTEXT NOT NULL COMMENT '关联的用户ID列表（JSON数组）',
    `creator` VARCHAR(36) NOT NULL COMMENT '创建人ID',
    `is_del` INT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    `ctime` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `utime` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限规则组表';