package com.datacopilotx.ai.domian.bean;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.sql.Timestamp;

@Data
@TableName("MODEL_CONFIG")
public class ModelConfigBean {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    // 模型名称
    private String model;
    @TableField("api_key")
    private String apiKey;
    // 模型api协议格类型，openai、ollama
    private String type;
    // 模型功能类型，embedding、chat
    @TableField("function_type")
    private String functionType;
    // 嵌入维度
    private Integer dimension;
    // 模型api基础url
    @TableField("base_url")
    private String baseUrl;
    // 模型代理平台，硅基、qwen、ollama
    private String platform;
    private Timestamp utime;
    private Timestamp ctime;
    @TableField("is_del")
    @TableLogic
    private Integer isDel;
}
