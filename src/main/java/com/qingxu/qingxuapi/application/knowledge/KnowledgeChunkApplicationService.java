package com.qingxu.qingxuapi.application.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qingxu.qingxuapi.common.exception.BusinessException;
import com.qingxu.qingxuapi.common.response.ErrorCode;
import com.qingxu.qingxuapi.common.response.PageResponse;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KnowledgeChunkEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KnowledgeChunkMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KnowledgeDocumentMapper;
import com.qingxu.qingxuapi.interfaces.auth.dto.CurrentUserResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.dto.KnowledgeChunkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeChunkApplicationService {

    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeChunkMapper chunkMapper;

    public PageResponse<KnowledgeChunkResponse> chunks(Long documentId, long page, long pageSize, String keyword, String contentType, CurrentUserResponse currentUser) {
        KnowledgeDocumentEntity document = documentMapper.selectById(documentId);
        if (document == null || !currentUser.id().equals(document.getCreatedBy())) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND);
        }
        LambdaQueryWrapper<KnowledgeChunkEntity> wrapper = new LambdaQueryWrapper<KnowledgeChunkEntity>()
                .eq(KnowledgeChunkEntity::getDocumentId, documentId)
                .eq(KnowledgeChunkEntity::getDeleted, 0)
                .orderByAsc(KnowledgeChunkEntity::getChunkIndex);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(KnowledgeChunkEntity::getContent, keyword.trim());
        }
        if (contentType != null && !contentType.isBlank()) {
            wrapper.eq(KnowledgeChunkEntity::getContentType, contentType.trim());
        }
        Page<KnowledgeChunkEntity> result = chunkMapper.selectPage(Page.of(page, pageSize), wrapper);
        return new PageResponse<>(
                result.getRecords().stream().map(this::toResponse).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    private KnowledgeChunkResponse toResponse(KnowledgeChunkEntity chunk) {
        return new KnowledgeChunkResponse(
                chunk.getId(),
                chunk.getChunkIndex(),
                chunk.getContent(),
                chunk.getContentType(),
                chunk.getTitlePath(),
                chunk.getPageNumber(),
                chunk.getSheetName(),
                chunk.getTokenCount(),
                chunk.getEmbeddingStatus()
        );
    }
}
