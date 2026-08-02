package com.qingxu.qingxuapi.interfaces.knowledge.dto;

public record KnowledgeGraphSourceChunkResponse(
        Long chunkId,
        Long documentId,
        Long fileId,
        Integer chunkIndex,
        String mentionText,
        String content,
        String titlePath,
        Integer pageNumber
) {
}
