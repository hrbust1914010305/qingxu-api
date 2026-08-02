package com.qingxu.qingxuapi.interfaces.knowledge.v2.dto;

public record KnowledgeV2RemoveResponse(
        Long knowledgeId,
        Integer removedDocuments,
        Integer removedFiles,
        String message
) {
}
