package com.qingxu.qingxuapi.interfaces.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        String username,

        @NotBlank(message = "昵称不能为空")
        String nickname,

        @Email(message = "邮箱格式不正确")
        String email,

        String phone,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, message = "密码长度不能少于8位")
        String password,

        @NotBlank(message = "确认密码不能为空")
        String confirmPassword,

        @NotBlank(message = "验证码标识不能为空")
        String captchaKey,

        @NotBlank(message = "验证码不能为空")
        String captcha,

        @AssertTrue(message = "必须同意用户协议和隐私政策")
        boolean agreePolicy
) {
}