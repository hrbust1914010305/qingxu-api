package com.qingxu.qingxuapi.interfaces.menu.dto;

public record MenuUpdateRequest(
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
