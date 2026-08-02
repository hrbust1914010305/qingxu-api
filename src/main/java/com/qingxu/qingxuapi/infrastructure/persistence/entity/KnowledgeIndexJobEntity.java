package com.qingxu.qingxuapi.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_index_job")
public class KnowledgeIndexJobEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private Long fileId;
    private Long knowledgeBaseId;
    private String status;
    private String stage;
    private Integer progress;
    private Integer estimatedSeconds;
    private Integer estimatedRemainingSeconds;
    private Integer retryCount;
    private Integer maxRetryCount;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
