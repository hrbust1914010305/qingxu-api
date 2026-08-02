package com.qingxu.qingxuapi.interfaces.knowledge.v2.dto;

import java.time.LocalDateTime;
import java.util.List;

public record KnowledgeV2ListItemResponse(
        Long knowledgeId,
        String name,
        String description,
        String visibility,
        String graphDomain,
        String status,
        Integer documentCount,
        Integer indexedDocumentCount,
        Integer failedDocumentCount,
        Integer chunkCount,
        List<KnowledgeV2AttachmentResponse> attachments,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
