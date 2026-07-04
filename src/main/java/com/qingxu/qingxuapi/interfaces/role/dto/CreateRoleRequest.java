package com.qingxu.qingxuapi.interfaces.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(
        @NotBlank(message = "角色编码不能为空")
        @Size(max = 50, message = "角色编码长度不能超过50个字符")
        String code,

        @NotBlank(message = "角色名称不能为空")
        @Size(max = 100, message = "角色名称长度不能超过100个字符")
        String name,

        String status,

        @Size(max = 255, message = "描述长度不能超过255个字符")
        String description,

        @Size(max = 500, message = "备注长度不能超过500个字符")
        String remark,

        Integer sortOrder
) {
}