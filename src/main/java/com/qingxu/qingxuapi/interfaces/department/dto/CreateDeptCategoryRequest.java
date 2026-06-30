package com.qingxu.qingxuapi.interfaces.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDeptCategoryRequest(
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 64, message = "分类名称长度不能超过64个字符")
        String name,

        @NotBlank(message = "分类编码不能为空")
        @Size(max = 64, message = "分类编码长度不能超过64个字符")
        String code,

        Integer sortOrder,
        String status
) {
}