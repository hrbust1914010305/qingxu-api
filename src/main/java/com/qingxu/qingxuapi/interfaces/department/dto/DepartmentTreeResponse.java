package com.qingxu.qingxuapi.interfaces.department.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DepartmentTreeResponse(
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
        List<DepartmentTreeResponse> children
) {
}