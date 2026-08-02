package com.qingxu.qingxuapi.interfaces.knowledge.v2.dto;

import java.util.List;

public record KnowledgeV2ProgressResponse(
        Long knowledgeId,
        String status,
        Boolean running,
        Integer estimatedRemainingSeconds,
        String message,
        Integer totalDocuments,
        Integer finishedDocuments,
        Integer failedDocuments,
        Integer canceledDocuments,
        Integer progress,
        List<KnowledgeV2StageProgressResponse> stages,
        List<KnowledgeV2ProgressDocumentResponse> documents
) {
}
