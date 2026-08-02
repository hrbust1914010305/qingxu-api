package com.qingxu.qingxuapi.interfaces.knowledge.v2.dto;

import java.time.LocalDateTime;

public record KnowledgeV2ProgressDocumentResponse(
        Long documentId,
        Long fileId,
        String fileName,
        Long jobId,
        Long latestJobId,
        String status,
        String stage,
        Integer progress,
        Boolean running,
        Integer estimatedRemainingSeconds,
        Integer chunkCount,
        Boolean graphReady,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String message
) {
}
