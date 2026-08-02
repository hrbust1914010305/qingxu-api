package com.qingxu.qingxuapi.infrastructure.ai;

public record KnowledgeGraphRuntimeStatusResponse(
        Long graphJobId,
        Boolean running,
        String status
) {
}
