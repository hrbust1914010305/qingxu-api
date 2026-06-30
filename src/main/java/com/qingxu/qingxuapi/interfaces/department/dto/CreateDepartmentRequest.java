package com.qingxu.qingxuapi.interfaces.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(
        @NotNull(message = "父部门ID不能为空")
        Long parentId,

        @NotBlank(message = "部门名称不能为空")
        @Size(max = 64, message = "部门名称长度不能超过64个字符")
        String name,

        @Size(max = 64, message = "负责人长度不能超过64个字符")
        String leader,

        @Size(max = 32, message = "联系电话长度不能超过32个字符")
        String phone,

        @Size(max = 128, message = "邮箱长度不能超过128个字符")
        String email,

        Integer sortOrder,
        String status,

        @Size(max = 512, message = "描述长度不能超过512个字符")
        String description
) {
}