package com.qingxu.qingxuapi.interfaces.knowledge.v2.dto;

public record KnowledgeV2StopParseResponse(
        Long knowledgeId,
        Integer canceledDocuments,
        String message
) {
}
