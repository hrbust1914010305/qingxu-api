package com.qingxu.qingxuapi.interfaces.knowledge.dto;

import java.util.List;
import java.util.Map;

public record KnowledgeIndexedChunkRequest(
        Integer chunkIndex,
        String content,
        String contentType,
        List<String> titlePath,
        Integer pageNumber,
        String sheetName,
        Integer slideNumber,
        Integer startElementIndex,
        Integer endElementIndex,
        Integer tokenCount,
        String chunkHash,
        Map<String, Object> metadata,
        String embeddingModel,
        Integer embeddingDimension,
        List<Double> embedding
) {
}
