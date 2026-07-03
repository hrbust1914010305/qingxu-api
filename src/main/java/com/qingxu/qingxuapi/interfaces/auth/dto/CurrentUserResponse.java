package com.qingxu.qingxuapi.interfaces.auth.dto;

import java.util.List;

public record CurrentUserResponse(
        Long id,
        String username,
        String realname,
        String nickname,
        String email,
        String phone,
        String tenantId,
        String userType,
        String status,
        Boolean needPasswordChange,
        List<String> roles,
        List<String> permissions
) {
}
