package com.qingxu.qingxuapi.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingxu.qingxuapi.common.response.ErrorCode;
import com.qingxu.qingxuapi.common.response.ResponseFactory;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityResponseWriter {

    private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";

    private final ResponseFactory responseFactory;
    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(JSON_CONTENT_TYPE);
        objectMapper.writeValue(response.getWriter(), responseFactory.failure(errorCode));
    }
}
