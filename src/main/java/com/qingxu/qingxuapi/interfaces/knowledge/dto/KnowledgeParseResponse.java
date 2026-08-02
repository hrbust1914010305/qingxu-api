package com.qingxu.qingxuapi.interfaces.knowledge.dto;

public record KnowledgeParseResponse(
        Long documentId,
        Long jobId,
        Long fileId,
        Long knowledgeBaseId,
        String status,
        String stage,
        Integer progress,
        Integer estimatedSeconds,
        Integer estimatedRemainingSeconds,
        String message
) {
}
