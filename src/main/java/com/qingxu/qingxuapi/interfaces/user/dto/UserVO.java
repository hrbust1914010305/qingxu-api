package com.qingxu.qingxuapi.interfaces.user.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserVO(
        Long id,
        String username,
        String realname,
        String nickname,
        String avatar,
        List<UploadFileItemVO> avatarFiles,
        String email,
        String phone,
        String userType,
        String status,
        List<Long> deptIds,
        List<String> deptNames,
        List<Long> roleIds,
        List<String> roleNames,
        Boolean needPasswordChange,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
