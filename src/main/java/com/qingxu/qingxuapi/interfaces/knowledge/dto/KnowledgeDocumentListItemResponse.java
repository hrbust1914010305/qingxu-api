package com.qingxu.qingxuapi.interfaces.knowledge.dto;

import java.time.LocalDateTime;

public record KnowledgeDocumentListItemResponse(
        Long documentId,
        Long fileId,
        Long knowledgeBaseId,
        String fileName,
        String mimeType,
        String extension,
        Long size,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String status,
        String stage,
        Integer progress,
        Integer estimatedSeconds,
        Integer estimatedRemainingSeconds,
        Integer chunkCount,
        Boolean graphReady,
        String errorMessage,
        Long latestJobId
) {
}
