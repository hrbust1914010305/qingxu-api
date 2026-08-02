package com.qingxu.qingxuapi.interfaces.knowledge.dto;

public record KnowledgeChunkResponse(
        Long id,
        Integer chunkIndex,
        String content,
        String contentType,
        String titlePath,
        Integer pageNumber,
        String sheetName,
        Integer tokenCount,
        String embeddingStatus
) {
}
