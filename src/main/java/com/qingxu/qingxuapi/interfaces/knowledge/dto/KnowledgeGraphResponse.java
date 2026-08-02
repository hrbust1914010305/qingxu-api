package com.qingxu.qingxuapi.interfaces.knowledge.dto;

import java.util.List;

public record KnowledgeGraphResponse(
        List<KnowledgeGraphNodeResponse> nodes,
        List<KnowledgeGraphEdgeResponse> edges
) {
}
