package com.qingxu.qingxuapi.interfaces.knowledge.v2.dto;

public record KnowledgeV2StageProgressResponse(
        String stage,
        String name,
        String status,
        Integer progress,
        Boolean current,
        Integer estimatedRemainingSeconds
) {
}
