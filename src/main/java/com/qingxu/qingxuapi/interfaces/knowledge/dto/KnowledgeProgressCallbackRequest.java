package com.qingxu.qingxuapi.interfaces.knowledge.dto;

public record KnowledgeProgressCallbackRequest(
        Long jobId,
        Long documentId,
        String status,
        String stage,
        Integer progress,
        Integer estimatedRemainingSeconds,
        String message,
        Integer chunkCount,
        Boolean graphReady,
        String errorMessage,
        java.util.List<KnowledgeIndexedChunkRequest> chunks
) {
}
