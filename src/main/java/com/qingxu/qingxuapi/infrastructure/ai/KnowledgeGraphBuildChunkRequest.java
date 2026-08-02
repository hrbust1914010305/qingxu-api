package com.qingxu.qingxuapi.infrastructure.ai;

public record KnowledgeGraphBuildChunkRequest(
        Long chunkId,
        Integer chunkIndex,
        String content,
        String titlePath,
        Integer pageNumber,
        String sheetName,
        Integer slideNumber
) {
}
