package com.datacopilotx.ai.controller.advice;


import com.datacopilotx.common.exception.DataCopilotXException;
import com.datacopilotx.common.result.WebResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


@Slf4j
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExceptionControllerAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<WebResult<String>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        // 获取第一个字段错误
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String errorMessage = fieldError != null ?
                fieldError.getField() + ": " + fieldError.getDefaultMessage() :
                "参数校验失败";

        log.warn("参数校验失败: URL={}, 错误信息={}", request.getRequestURI(), errorMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(WebResult.error(HttpStatus.BAD_REQUEST.value(), errorMessage));
    }

    /**
     * 处理参数类型不匹配异常 - MethodArgumentTypeMismatchException
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    public ResponseEntity<WebResult<String>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String errorMessage = String.format("参数'%s'类型不匹配，应为%s类型",
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "未知");

        log.warn("参数类型不匹配: URL={}, 错误信息={}", request.getRequestURI(), errorMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(WebResult.error(HttpStatus.BAD_REQUEST.value(), errorMessage));
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler({RuntimeException.class})
    @ResponseBody
    public ResponseEntity<?> runtimeExceptionHandler(RuntimeException runtimeException, HttpServletRequest request) {
        if (isSseEndpoint(request)) {
            log.error("Exception occurred in SSE endpoint, request URI: {}", request.getRequestURI(), runtimeException);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        Throwable r = getRootCause(runtimeException);
        WebResult result = WebResult.error(r);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body(result);
    }

    private boolean isSseEndpoint(HttpServletRequest request) {
        return "/chat/completions".equals(request.getRequestURI());
    }


    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler({DataCopilotXException.class})
    @ResponseBody
    public ResponseEntity<?> dataCopilotXExceptionHandler(DataCopilotXException dataCopilotXException) {
        Throwable r = getRootCause(dataCopilotXException);
        WebResult result = WebResult.error(r);
        return ResponseEntity
                .status(HttpStatus.OK.value())
                .body(result);
    }

    public static Throwable getRootCause(Throwable ex) {
        return ex.getCause() != null ? getRootCause(ex.getCause()) : ex;
    }
}
