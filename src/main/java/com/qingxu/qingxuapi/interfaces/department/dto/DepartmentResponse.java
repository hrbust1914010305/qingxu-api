package com.qingxu.qingxuapi.interfaces.department.dto;

import java.time.LocalDateTime;

public record DepartmentResponse(
        Long id,
        String tenantId,
        Long parentId,
        String name,
        String deptType,
        Long categoryId,
        String leader,
        String phone,
        String email,
        Integer sortOrder,
        String status,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}