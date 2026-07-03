package com.qingxu.qingxuapi.interfaces.department.dto;

public record LeaderUser(
        Long id,
        String username,
        String realname,
        String nickname,
        String phone,
        String email
) {
}
