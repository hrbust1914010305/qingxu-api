package com.qingxu.qingxuapi.infrastructure.ai;

public record KnowledgeGraphBuildAcceptedResponse(
        Boolean accepted,
        Long graphJobId,
        String message
) {
}
