package com.qingxu.qingxuapi.infrastructure.ai;

public record KnowledgeIndexStartRequest(
        Long jobId,
        Long documentId,
        Long fileId,
        Long knowledgeBaseId,
        String storagePath,
        String originalName,
        String mimeType,
        String extension,
        String checksum,
        Long createdBy
) {
}
