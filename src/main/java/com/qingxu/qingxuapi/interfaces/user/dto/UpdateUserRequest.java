package com.qingxu.qingxuapi.interfaces.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateUserRequest(
        @Size(max = 64, message = "昵称长度不能超过64个字符")
        String nickname,

        @Size(max = 64, message = "真实姓名长度不能超过64个字符")
        String realname,

        String avatar,

        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @Email(message = "邮箱格式不正确")
        String email,

        String userType,

        String status,

        List<Long> deptIds,

        List<Long> roleIds
) {
}
