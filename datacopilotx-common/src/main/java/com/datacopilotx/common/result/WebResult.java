package com.datacopilotx.common.result;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * 统一Web响应结果封装类
 * @param <T> 响应数据类型
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
@Data
public class WebResult<T> {
    private Integer code;
    private String message;
    private String traceId;
    private T data;

    public WebResult() {
    }

    /**
     * 构造函数
     * @param code 状态
     * @param errstr 错误信息
     * @param result 结果数据
     */
    public WebResult(Integer code, String errstr, T result) {
        this.code = code;
        this.message = errstr;
        this.data = result;
        this.traceId = MDC.get("traceId");
    }

    /**
     * 创建成功响应
     * @param result 结果数据
     * @param <T> 数据类型
     * @return WebResult实例
     */
    public static <T> WebResult<T> success(T result) {
        return new WebResult<>(ResponseCode.RESPONSE_SUCCESS.getValue(), null, result);
    }

    /**
     * 创建成功响应（无数据）
     * @param <T> 数据类型
     * @return WebResult实例
     */
    public static <T> WebResult<T> success() {
        return new WebResult<>(ResponseCode.RESPONSE_SUCCESS.getValue(), null, null);
    }

    /**
     * 创建错误响应
     * @param errstr 错误信息
     * @param <T> 数据类型
     * @return WebResult实例
     */
    public static <T> WebResult<T> error(Integer code, String errstr) {
        return new WebResult<>(code, errstr, null);
    }

    public static <T> WebResult<T> error(Throwable throwable) {
        log.error(throwable.getMessage(), throwable);
        WebResult<T> r = new WebResult<T>();
        r.setCode(ResponseCode.SYSTEM_ERROR.getValue());
        r.setMessage(throwable.getMessage());
        return r;
    }


    @Override
    public String toString() {
        return "WebResult{" +
                "status='" + code + '\'' +
                ", errstr='" + message + '\'' +
                ", traceId='" + traceId + '\'' +
                ", result=" + data +
                '}';
    }
}
