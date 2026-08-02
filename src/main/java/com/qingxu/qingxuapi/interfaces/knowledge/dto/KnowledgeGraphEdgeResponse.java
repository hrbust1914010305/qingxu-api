package com.qingxu.qingxuapi.interfaces.knowledge.dto;

import java.math.BigDecimal;

public record KnowledgeGraphEdgeResponse(
        Long id,
        Long sourceEntityId,
        Long targetEntityId,
        String relationType,
        String relationLabelZh,
        String description,
        BigDecimal confidence
) {
}
