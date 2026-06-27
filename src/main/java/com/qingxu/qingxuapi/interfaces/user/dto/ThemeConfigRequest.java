package com.qingxu.qingxuapi.interfaces.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ThemeConfigRequest(
        @NotBlank(message = "布局类型不能为空")
        String layoutType,
        @NotBlank(message = "主题色不能为空")
        @Size(max = 32, message = "主题色长度不能超过32个字符")
        String themeColor,
        Boolean colorWeakMode,
        Boolean grayMode,
        Boolean asideDark,
        Boolean darkMode,
        @NotBlank(message = "页面过渡动画不能为空")
        String transitionPage
) {
}