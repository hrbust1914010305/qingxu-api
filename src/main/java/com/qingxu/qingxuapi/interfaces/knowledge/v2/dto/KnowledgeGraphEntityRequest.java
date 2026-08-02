package com.qingxu.qingxuapi.interfaces.knowledge.v2.dto;

import java.math.BigDecimal;
import java.util.List;

public record KnowledgeGraphEntityRequest(
        String name,
        String normalizedName,
        String entityType,
        String description,
        BigDecimal confidence,
        List<String> aliases,
        List<KnowledgeGraphEntityMentionRequest> mentions
) {
}
