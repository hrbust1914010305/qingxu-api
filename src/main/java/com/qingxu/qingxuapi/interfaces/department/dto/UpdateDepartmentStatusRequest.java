package com.qingxu.qingxuapi.interfaces.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateDepartmentStatusRequest(
        @NotBlank(message = "状态不能为空")
        @Pattern(regexp = "^ACTIVE$|^DISABLED$", message = "状态只能是ACTIVE或DISABLED")
        String status
) {
}