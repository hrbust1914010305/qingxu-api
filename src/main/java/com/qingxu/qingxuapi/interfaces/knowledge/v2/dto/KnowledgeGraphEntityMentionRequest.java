package com.qingxu.qingxuapi.interfaces.knowledge.v2.dto;

import java.math.BigDecimal;

public record KnowledgeGraphEntityMentionRequest(
        Long chunkId,
        String mentionText,
        String titlePath,
        Integer pageNumber,
        Integer startOffset,
        Integer endOffset,
        BigDecimal confidence
) {
}
