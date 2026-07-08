package com.qingxu.qingxuapi.interfaces.file.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record FileUploadStatusResponse(
        String uploadId,
        String status,
        Long chunkSize,
        Integer totalChunks,
        List<Integer> uploadedChunks,
        OffsetDateTime expiresAt
) {
}
