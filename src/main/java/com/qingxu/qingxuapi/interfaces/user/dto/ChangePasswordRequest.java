package com.qingxu.qingxuapi.interfaces.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "旧密码不能为空")
        String oldPassword,

        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, message = "新密码长度不能少于8位")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "新密码必须包含大写字母、小写字母、数字和特殊字符（如!@#$%等）")
        String newPassword,

        @NotBlank(message = "确认密码不能为空")
        String confirmPassword
) {
}