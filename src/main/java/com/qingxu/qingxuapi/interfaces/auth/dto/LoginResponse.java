package com.qingxu.qingxuapi.interfaces.auth.dto;

public record LoginResponse(
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
    public static LoginResponse from(CurrentUserResponse currentUser) {
        return new LoginResponse(
                currentUser.id(),
                currentUser.username(),
                currentUser.realname(),
                currentUser.nickname(),
                currentUser.avatar(),
                currentUser.email(),
                currentUser.phone(),
                currentUser.tenantId(),
                currentUser.userType(),
                currentUser.status(),
                currentUser.needPasswordChange()
        );
    }
}
