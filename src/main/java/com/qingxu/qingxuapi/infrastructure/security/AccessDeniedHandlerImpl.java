package com.qingxu.qingxuapi.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingxu.qingxuapi.common.response.ErrorCode;
import com.qingxu.qingxuapi.common.response.ResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    private final ResponseFactory responseFactory;
    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(ErrorCode.AUTH_403.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), responseFactory.failure(ErrorCode.AUTH_403));
    }
}
