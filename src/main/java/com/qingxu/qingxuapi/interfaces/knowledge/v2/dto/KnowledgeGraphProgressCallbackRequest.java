package com.qingxu.qingxuapi.interfaces.knowledge.v2.dto;

import java.util.List;

public record KnowledgeGraphProgressCallbackRequest(
        Long graphJobId,
        Long documentId,
        Long knowledgeBaseId,
        String status,
        String stage,
        Integer progress,
        String message,
        String errorMessage,
        List<KnowledgeGraphEntityRequest> entities,
        List<KnowledgeGraphRelationRequest> relations
) {
}
