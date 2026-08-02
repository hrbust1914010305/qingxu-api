package com.qingxu.qingxuapi.interfaces.knowledge.v2.dto;

import java.util.List;

public record KnowledgeV2CreateResponse(
        Long knowledgeId,
        String name,
        String description,
        String visibility,
        String graphDomain,
        Integer documentCount,
        List<KnowledgeV2AttachmentResponse> attachments
) {
}
