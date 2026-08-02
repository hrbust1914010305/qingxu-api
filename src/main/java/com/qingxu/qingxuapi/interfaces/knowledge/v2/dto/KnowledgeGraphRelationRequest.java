package com.qingxu.qingxuapi.interfaces.knowledge.v2.dto;

import java.math.BigDecimal;

public record KnowledgeGraphRelationRequest(
        String sourceName,
        String sourceType,
        String targetName,
        String targetType,
        String relationType,
        String relationLabelZh,
        String description,
        String evidenceText,
        Long chunkId,
        BigDecimal confidence
) {
}
