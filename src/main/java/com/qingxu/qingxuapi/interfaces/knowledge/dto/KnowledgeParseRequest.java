package com.qingxu.qingxuapi.interfaces.knowledge.dto;

import jakarta.validation.constraints.NotNull;

public record KnowledgeParseRequest(@NotNull Long fileId, Long knowledgeBaseId) {
}
