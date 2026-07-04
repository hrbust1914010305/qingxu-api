package com.qingxu.qingxuapi.interfaces.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MenuCreateRequest(
        Long parentId,

        @NotBlank(message = "菜单名称不能为空")
        String name,

    /**
     * 对于 BUTTON 类型的菜单可以为空，后端会自动填充空字符串。
     * 对于 MENU、DIRECTORY 必须非空，由 validatePath 方法统一校验。
     */
    String path,

        String component,

        String redirect,

        @NotBlank(message = "菜单标题不能为空")
        String title,

        String icon,

        String permission,

        @NotBlank(message = "菜单类型不能为空")
        String menuType,

        Integer sortOrder,

        Boolean visible,

        String status,

        Boolean isExternal,

        Boolean isCache
) {
    public Long getParentId() { return parentId; }
    public String getName() { return name; }
    public String getPath() { return path; }
    public String getComponent() { return component; }
    public String getRedirect() { return redirect; }
    public String getTitle() { return title; }
    public String getIcon() { return icon; }
    public String getPermission() { return permission; }
    public String getMenuType() { return menuType; }
    public Integer getSortOrder() { return sortOrder; }
    public Boolean getVisible() { return visible; }
    public String getStatus() { return status; }
    public Boolean getIsExternal() { return isExternal; }
    public Boolean getIsCache() { return isCache; }
}
