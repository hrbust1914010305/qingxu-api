package com.qingxu.qingxuapi.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qingxu.qingxuapi.infrastructure.persistence.typehandler.JsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "knowledge_chunk", autoResultMap = true)
public class KnowledgeChunkEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long knowledgeBaseId;
    private Long documentId;
    private Long fileId;
    private Integer chunkIndex;
    private Long parentChunkId;
    private String content;
    private String contentType;
    private String titlePath;
    private Integer pageNumber;
    private String sheetName;
    private Integer slideNumber;
    private Integer startElementIndex;
    private Integer endElementIndex;
    private Integer startOffset;
    private Integer endOffset;
    private Integer tokenCount;
    private String chunkHash;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String metadataJson;
    private String embeddingModel;
    private Integer embeddingDimension;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String embeddingJson;
    private String embeddingStatus;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
