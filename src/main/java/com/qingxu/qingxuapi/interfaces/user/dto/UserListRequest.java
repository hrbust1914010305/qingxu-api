package com.qingxu.qingxuapi.interfaces.user.dto;

public record UserListRequest(
        Integer page,
        Integer pageSize,
        String username,
        String phone,
        String userType,
        String status,
        Long deptId
) {
    public int getPage() {
        return page != null && page > 0 ? page : 1;
    }

    public int getPageSize() {
        return pageSize != null && pageSize > 0 ? pageSize : 10;
    }
}
