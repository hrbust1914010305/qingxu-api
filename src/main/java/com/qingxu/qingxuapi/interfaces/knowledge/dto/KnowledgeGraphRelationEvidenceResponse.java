package com.qingxu.qingxuapi.interfaces.knowledge.dto;

import java.math.BigDecimal;

public record KnowledgeGraphRelationEvidenceResponse(
        Long id,
        Long documentId,
        Long fileId,
        Long chunkId,
        String evidenceText,
        BigDecimal confidence
) {
}
