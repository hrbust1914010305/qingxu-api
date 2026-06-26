package com.qingxu.qingxuapi.interfaces.auth.dto;

import java.util.List;

public record PermissionResponse(
        List<String> roles,
        List<String> permissions
) {
}