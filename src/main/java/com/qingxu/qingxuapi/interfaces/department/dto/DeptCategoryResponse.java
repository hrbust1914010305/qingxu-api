package com.qingxu.qingxuapi.interfaces.department.dto;

import java.time.LocalDateTime;

public record DeptCategoryResponse(
        Long id,
        String tenantId,
        String name,
        String code,
        Integer sortOrder,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}