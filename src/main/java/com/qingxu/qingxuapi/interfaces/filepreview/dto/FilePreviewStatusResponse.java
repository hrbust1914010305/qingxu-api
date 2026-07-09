package com.qingxu.qingxuapi.interfaces.filepreview.dto;

public record FilePreviewStatusResponse(
        Long fileId,
        String status,
        String message
) {
}
