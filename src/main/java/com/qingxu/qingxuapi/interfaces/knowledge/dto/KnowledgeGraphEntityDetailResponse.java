package com.qingxu.qingxuapi.interfaces.knowledge.dto;

import java.math.BigDecimal;
import java.util.List;

public record KnowledgeGraphEntityDetailResponse(
        Long id,
        Long knowledgeBaseId,
        String entityType,
        String name,
        String normalizedName,
        String description,
        BigDecimal confidence,
        List<String> aliases,
        List<KnowledgeGraphSourceChunkResponse> sourceChunks
) {
}
