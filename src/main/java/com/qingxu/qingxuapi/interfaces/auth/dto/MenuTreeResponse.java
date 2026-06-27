package com.qingxu.qingxuapi.interfaces.auth.dto;

import java.util.List;

public record MenuTreeResponse(
        Long id,
        Long parentId,
        String name,
        String path,
        String component,
        String redirect,
        String title,
        String icon,
        String menuType,
        Integer sortOrder,
        Boolean visible,
        String status,
        Boolean isExternal,
        Boolean isCache,
        List<MenuTreeResponse> children
) {
}