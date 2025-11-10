package com.datacopilotx.common.result;

import lombok.Getter;

public enum ResponseCode {

    RESPONSE_SUCCESS(200),
    SYSTEM_ERROR(500, "系统内部错误，请联系管理员"),

    UNSELECT_DATASOURCE(5001, "未指定数据源"),
    UNKNOWN_DATASOURCE(5002, "指定数据源不存在"),
    MODEL_CONNECTION_FAILED(6001, "模型连接失败"),
    MODEL_NOT_FOUND(6002, "模型不存在"),
    MODEL_REPETITION(6000, "模型重复"),
    DATASET_NOT_FOUND(6100, "数据集不存在"),
    DATASET_REPEAT(6101, "数据集名称重复"),
    VECTOR_EMBEDDING_FAILED(7000, "获取向量化数据失败"),
    COMPONENT_FAILED(8000, "知识库组件失败"),
    KNOWLEDGE_LIB_NOT_FOUND(8001, "知识库不存在"),
    KNOWLEDGE_LIB_CONFIG_ERROR(8002, "知识库配置错误")
    ;

    private final int value;

    @Getter
    private String msg;


    ResponseCode(int code) {
        this.value = code;
    }

    ResponseCode(int code, String msg) {
        this.value = code;
        this.msg = msg;
    }


    public int getValue() {
        return this.value;
    }

    public static ResponseCode getByCode(int code) {
        for (ResponseCode enums : ResponseCode.values()) {
            if (enums.getValue() == code) {
                return enums;
            }
        }
        return null;
    }
}
