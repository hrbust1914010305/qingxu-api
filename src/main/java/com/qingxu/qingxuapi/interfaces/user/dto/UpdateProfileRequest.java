package com.qingxu.qingxuapi.interfaces.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "昵称不能为空")
        @Size(max = 64, message = "昵称长度不能超过64个字符")
        String nickname,

        @Size(max = 64, message = "真实姓名长度不能超过64个字符")
        String realname,

        String avatar,

        @Email(message = "邮箱格式不正确")
        String email,

        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone
) {
}
