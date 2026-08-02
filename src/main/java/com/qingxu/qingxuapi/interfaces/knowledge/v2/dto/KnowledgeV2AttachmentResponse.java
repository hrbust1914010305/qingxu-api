package com.qingxu.qingxuapi.interfaces.knowledge.v2.dto;

import java.time.LocalDateTime;

public record KnowledgeV2AttachmentResponse(
        Long documentId,
        Long fileId,
        String originalName,
        String storagePath,
        String mimeType,
        String extension,
        Long size,
        String checksum,
        String bizType,
        String documentStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
