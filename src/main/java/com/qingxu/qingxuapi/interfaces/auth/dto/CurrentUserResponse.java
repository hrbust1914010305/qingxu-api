package com.qingxu.qingxuapi.interfaces.auth.dto;

public record CurrentUserResponse(
        Long id,
        String username,
        String realname,
        String nickname,
        String avatar,
        String email,
        String phone,
        String tenantId,
        String userType,
        String status,
        Boolean needPasswordChange
) {
}
