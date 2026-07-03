package com.qingxu.qingxuapi.interfaces.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ToggleStatusRequest(
        @NotBlank(message = "状态不能为空")
        String status
) {
}
