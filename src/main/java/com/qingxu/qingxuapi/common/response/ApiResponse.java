package com.qingxu.qingxuapi.common.response;

import java.time.OffsetDateTime;

public record ApiResponse<T>(
        String code,
        String message,
        T data,
        String traceId,
        OffsetDateTime timestamp
) {
}
