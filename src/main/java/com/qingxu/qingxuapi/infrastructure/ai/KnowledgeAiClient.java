package com.qingxu.qingxuapi.infrastructure.ai;

public interface KnowledgeAiClient {
    void ensureAvailable();

    void startIndexing(KnowledgeIndexStartRequest request);

    KnowledgeGraphBuildAcceptedResponse startGraphBuild(KnowledgeGraphBuildStartRequest request);

    KnowledgeGraphRuntimeStatusResponse graphRuntimeStatus(Long graphJobId);

    void cancelIndexing(Long jobId);

    default KnowledgeIndexAcceptedResponse startIndexingAccepted(KnowledgeIndexStartRequest request) {
        startIndexing(request);
        return new KnowledgeIndexAcceptedResponse(true, request.jobId(), "index job accepted");
    }
}
