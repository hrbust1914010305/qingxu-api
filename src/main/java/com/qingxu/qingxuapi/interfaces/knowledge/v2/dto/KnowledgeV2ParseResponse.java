package com.qingxu.qingxuapi.interfaces.knowledge.v2.dto;

public record KnowledgeV2ParseResponse(
        Long knowledgeId,
        Integer totalDocuments,
        Integer startedDocuments,
        Integer skippedDocuments,
        String message
) {
}
