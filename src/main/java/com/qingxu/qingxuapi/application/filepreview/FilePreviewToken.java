package com.qingxu.qingxuapi.application.filepreview;

import java.time.OffsetDateTime;

public record FilePreviewToken(
        String token,
        Long fileId,
        OffsetDateTime expiresAt
) {
}
