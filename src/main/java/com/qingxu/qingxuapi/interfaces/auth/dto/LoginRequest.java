package com.qingxu.qingxuapi.interfaces.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, message = "密码长度不能少于8位")
        String password,

        @NotBlank(message = "验证码标识不能为空")
        String captchaKey,

        @NotBlank(message = "验证码不能为空")
        String captcha
) {
}