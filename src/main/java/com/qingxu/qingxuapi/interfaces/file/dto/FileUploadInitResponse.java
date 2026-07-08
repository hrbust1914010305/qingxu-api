package com.qingxu.qingxuapi.interfaces.file.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record FileUploadInitResponse(
        String uploadId,
        Long chunkSize,
        Integer totalChunks,
        List<Integer> uploadedChunks,
        OffsetDateTime expiresAt
) {
}
