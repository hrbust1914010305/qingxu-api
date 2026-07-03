package com.qingxu.qingxuapi.interfaces.user.dto;

import java.util.List;

public record UserExportRequest(
        String username,
        String phone,
        String userType,
        String status,
        Long deptId,
        List<Long> ids
) {
}
