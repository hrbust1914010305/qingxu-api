package com.qingxu.qingxuapi.infrastructure.ai;

public record KnowledgeIndexCancelResponse(
        Boolean canceled,
        Long jobId,
        String message
) {
}
