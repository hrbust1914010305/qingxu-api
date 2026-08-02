package com.qingxu.qingxuapi.interfaces.knowledge.v2.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record KnowledgeV2CreateRequest(
        @NotBlank String name,
        String description,
        String visibility,
        String graphDomain,
        @Valid List<KnowledgeV2AttachmentRequest> attachments
) {
}
