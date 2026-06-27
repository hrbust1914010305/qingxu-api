package com.qingxu.qingxuapi.interfaces.user.dto;

public record UserPreferenceResponse(
        Long userId,
        SystemSettings systemSettings,
        ThemeConfig themeConfig
) {
}