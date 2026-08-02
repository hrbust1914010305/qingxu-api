package com.qingxu.qingxuapi.infrastructure.websocket;

public record KnowledgeIndexProgressPayload(
        String eventType,
        Long jobId,
        Long documentId,
        Long fileId,
        Long knowledgeBaseId,
        String status,
        String stage,
        Integer progress,
        Integer estimatedRemainingSeconds,
        String message,
        Integer chunkCount,
        Boolean graphReady,
        String errorMessage
) {
    public static final String EVENT_TYPE = "KNOWLEDGE_INDEX_PROGRESS";
}
