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
    `table` char(255) DEFAULT NULL COMMENT '数据表名',
    `type` varchar(255) NOT NULL DEFAULT '0' COMMENT '数据集类型',
    `fields` longtext CHARACTER SET utf8 NOT NULL COMMENT '数据集元数据',
    `ctime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `utime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_del` int(11) DEFAULT '0',
    `description` varchar(255) DEFAULT NULL COMMENT '数据集描述',
    `inject_prompt` longtext CHARACTER SET utf8 NOT NULL COMMENT '数据集注入prompt',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COMMENT='数据集信息';


CREATE TABLE `MODEL_CONFIG` (
    `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
    `model` char(255) CHARACTER SET utf8 DEFAULT NULL,
    `api_key` char(255) DEFAULT NULL COMMENT '模型api_key',
    `base_url` char(255) DEFAULT NULL COMMENT '模型交互地址',
    `platform` varchar(100) DEFAULT NULL,
    `config` text COMMENT '附加配置',
    `function_type` varchar(100) DEFAULT NULL COMMENT '模型功能类型，embedding、chat',
    `type` varchar(100) DEFAULT NULL COMMENT '模型类型',
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
     `description` varchar(255) DEFAULT NULL COMMENT '模型平台',
     `is_del` int(11) NOT NULL DEFAULT '0',
     `ctime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
     `utime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COMMENT='知识库配置';
