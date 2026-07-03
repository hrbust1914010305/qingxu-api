package com.qingxu.qingxuapi.interfaces.menu.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MenuTreeNode(
        Long id,
        Long parentId,
        String name,
        String path,
        String component,
        String redirect,
        String title,
        String icon,
        String permission,
        String menuType,
        Integer sortOrder,
        Boolean visible,
        String status,
        Boolean isExternal,
        Boolean isCache,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<MenuTreeNode> children
) {
}
