package com.qingxu.qingxuapi.interfaces.user.dto;

import jakarta.validation.Valid;

public record SavePreferenceRequest(
        @Valid SystemSettingsRequest systemSettings,
        @Valid ThemeConfigRequest themeConfig
) {
}