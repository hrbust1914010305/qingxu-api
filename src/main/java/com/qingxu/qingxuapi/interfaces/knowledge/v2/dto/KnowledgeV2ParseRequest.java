package com.qingxu.qingxuapi.interfaces.knowledge.v2.dto;

import jakarta.validation.constraints.NotNull;

public record KnowledgeV2ParseRequest(@NotNull Long knowledgeId) {
}
