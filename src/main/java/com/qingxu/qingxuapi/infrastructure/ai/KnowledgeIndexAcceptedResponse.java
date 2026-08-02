package com.qingxu.qingxuapi.infrastructure.ai;

public record KnowledgeIndexAcceptedResponse(
        Boolean accepted,
        Long jobId,
        String message
) {
}
