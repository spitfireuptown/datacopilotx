package com.datacopilotx.aigateway.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.function.Predicate;

@Slf4j
public class RetryHelper {

    public static final Predicate<Throwable> RETRYABLE_ERROR_PREDICATE = throwable -> {
        if (throwable instanceof WebClientResponseException responseException) {
            int statusCode = responseException.getStatusCode().value();
            if (statusCode >= 500) {
                log.warn("Server error ({}), will retry", statusCode);
                return true;
            }
            if (statusCode == 429) {
                log.warn("Rate limited (429), will retry");
                return true;
            }
            log.debug("Client error ({}), will not retry", statusCode);
            return false;
        }

        if (throwable instanceof SocketTimeoutException) {
            log.warn("Request timeout, will retry");
            return true;
        }

        if (throwable instanceof UnknownHostException) {
            log.warn("Unknown host, will retry");
            return true;
        }

        String message = throwable.getMessage();
        if (message != null && (message.contains("Connection reset")
                || message.contains("Connection refused")
                || message.contains("broken pipe")
                || message.contains("reset by peer"))) {
            log.warn("Connection error: {}, will retry", message);
            return true;
        }

        log.debug("Non-retryable error: {}", throwable.getClass().getName());
        return false;
    };
}