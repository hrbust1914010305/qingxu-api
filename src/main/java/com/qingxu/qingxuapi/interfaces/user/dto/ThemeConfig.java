package com.qingxu.qingxuapi.interfaces.user.dto;

public record ThemeConfig(
        String layoutType,
        String themeColor,
        Boolean colorWeakMode,
        Boolean grayMode,
        Boolean asideDark,
        Boolean darkMode,
        String transitionPage
) {
}