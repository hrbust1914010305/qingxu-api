package com.qingxu.qingxuapi.interfaces.auth.dto;

public record RouterResponse(
        String path,
        String name,
        String component,
        String title
) {
}
