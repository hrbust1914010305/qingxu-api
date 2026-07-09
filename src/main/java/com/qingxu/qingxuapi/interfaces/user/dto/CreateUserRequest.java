package com.qingxu.qingxuapi.interfaces.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateUserRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 64, message = "用户名长度2-64个字符")
        String username,

        @Size(max = 64, message = "昵称长度不能超过64个字符")
        String nickname,

        @Size(max = 64, message = "真实姓名长度不能超过64个字符")
        String realname,

        String avatar,

        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @jakarta.validation.constraints.Email(message = "邮箱格式不正确")
        String email,

        @NotBlank(message = "用户类型不能为空")
        String userType,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, message = "密码长度不能少于8位")
        String password,

        List<Long> deptIds,

        List<Long> roleIds
) {
}
