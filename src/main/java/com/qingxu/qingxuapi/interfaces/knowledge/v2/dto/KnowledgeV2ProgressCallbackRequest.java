package com.qingxu.qingxuapi.interfaces.knowledge.v2.dto;

import com.qingxu.qingxuapi.interfaces.knowledge.dto.KnowledgeIndexedChunkRequest;

import java.util.List;

public record KnowledgeV2ProgressCallbackRequest(
        Long jobId,
        Long documentId,
        String status,
        String stage,
        Integer progress,
        Integer estimatedRemainingSeconds,
        String message,
        Integer chunkCount,
        Boolean graphReady,
        String errorMessage,
        List<KnowledgeIndexedChunkRequest> chunks
) {
}
