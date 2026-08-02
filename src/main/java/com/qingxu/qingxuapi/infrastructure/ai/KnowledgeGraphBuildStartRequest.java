package com.qingxu.qingxuapi.infrastructure.ai;

import java.util.List;

public record KnowledgeGraphBuildStartRequest(
        Long graphJobId,
        Long indexJobId,
        Long knowledgeBaseId,
        Long documentId,
        Long fileId,
        Long createdBy,
        String graphDomain,
        List<KnowledgeGraphBuildChunkRequest> chunks
) {
}
