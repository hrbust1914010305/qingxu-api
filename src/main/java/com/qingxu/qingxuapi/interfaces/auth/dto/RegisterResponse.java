package com.qingxu.qingxuapi.interfaces.auth.dto;

public record RegisterResponse(
        boolean registered,
        String status,
        String message
) {
}
