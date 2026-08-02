package com.qingxu.qingxuapi.interfaces.knowledge.v2.dto;

import com.qingxu.qingxuapi.interfaces.knowledge.dto.KnowledgeGraphEdgeResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.dto.KnowledgeGraphNodeResponse;

import java.util.List;

public record KnowledgeV2GraphResponse(
        Long knowledgeId,
        String graphStatus,
        Boolean graphReady,
        Integer progress,
        String message,
        String errorMessage,
        List<KnowledgeGraphNodeResponse> nodes,
        List<KnowledgeGraphEdgeResponse> edges
) {
}
