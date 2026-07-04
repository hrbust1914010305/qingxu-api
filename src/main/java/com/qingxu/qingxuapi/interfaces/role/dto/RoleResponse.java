package com.qingxu.qingxuapi.interfaces.role.dto;

import java.time.LocalDateTime;

public record RoleResponse(
        Long id,
        String code,
        String name,
        String status,
        String description,
        String remark,
        Integer sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}