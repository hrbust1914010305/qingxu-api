package com.qingxu.qingxuapi.interfaces.department.dto;

import java.util.List;

public record DeleteDepartmentResponse(
        Integer successCount,
        Integer failCount,
        List<String> failReasons
) {
}