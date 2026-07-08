package com.qingxu.qingxuapi.common.response;

import com.qingxu.qingxuapi.common.config.TraceIdFilter;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Component
public class ResponseFactory {

    public <T> ApiResponse<T> success(T data) {
        return build(ErrorCode.SUCCESS, ErrorCode.SUCCESS.getMessage(), data);
    }

    public ApiResponse<Void> success() {
        return success(null);
    }

    public ApiResponse<Map<String, Long>> id(Long id) {
        return success(Map.of("id", id));
    }

    public ApiResponse<Void> failure(ErrorCode errorCode) {
        return failure(errorCode, errorCode.getMessage());
    }

    public ApiResponse<Void> failure(ErrorCode errorCode, String message) {
        return build(errorCode, message, null);
    }

    private <T> ApiResponse<T> build(ErrorCode errorCode, String message, T data) {
        return new ApiResponse<>(
                errorCode.getCode(),
                message,
                data,
                TraceIdFilter.currentTraceId(),
                OffsetDateTime.now(ZoneOffset.ofHours(8))
        );
    }
}
