package com.qingxu.qingxuapi.interfaces.department.dto;

import jakarta.validation.constraints.Size;

public record UpdateDeptCategoryRequest(
        @Size(max = 64, message = "分类名称长度不能超过64个字符")
        String name,

        Integer sortOrder,
        String status
) {
}