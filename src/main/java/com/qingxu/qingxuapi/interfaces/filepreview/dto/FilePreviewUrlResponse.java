package com.qingxu.qingxuapi.interfaces.filepreview.dto;

import java.time.OffsetDateTime;

public record FilePreviewUrlResponse(
        Long fileId,
        String fileName,
        String previewUrl,
        String status,
        OffsetDateTime expiresAt
) {
}
