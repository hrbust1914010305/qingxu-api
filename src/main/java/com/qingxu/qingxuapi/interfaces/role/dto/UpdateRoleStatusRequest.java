package com.qingxu.qingxuapi.interfaces.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateRoleStatusRequest(
        @NotBlank(message = "状态不能为空")
        @Pattern(regexp = "^ACTIVE$|^INACTIVE$", message = "状态只能是ACTIVE或INACTIVE")
        String status
) {
}