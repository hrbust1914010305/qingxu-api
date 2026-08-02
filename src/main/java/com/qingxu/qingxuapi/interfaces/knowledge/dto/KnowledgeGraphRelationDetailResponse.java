package com.qingxu.qingxuapi.interfaces.knowledge.dto;

import java.math.BigDecimal;
import java.util.List;

public record KnowledgeGraphRelationDetailResponse(
        Long id,
        Long knowledgeBaseId,
        KnowledgeGraphNodeResponse source,
        KnowledgeGraphNodeResponse target,
        String relationType,
        String relationLabelZh,
        String description,
        BigDecimal confidence,
        List<KnowledgeGraphRelationEvidenceResponse> evidences
) {
}
