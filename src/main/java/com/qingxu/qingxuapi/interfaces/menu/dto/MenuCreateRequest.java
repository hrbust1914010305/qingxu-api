package com.qingxu.qingxuapi.interfaces.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MenuCreateRequest(
        Long parentId,

        @NotBlank(message = "菜单名称不能为空")
        String name,

        @NotBlank(message = "路由路径不能为空")
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
