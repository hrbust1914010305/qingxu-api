package com.qingxu.qingxuapi.interfaces.knowledge.dto;

import java.math.BigDecimal;

public record KnowledgeGraphNodeResponse(
        Long id,
        String entityType,
        String name,
        String normalizedName,
        String description,
        BigDecimal confidence
) {
}
