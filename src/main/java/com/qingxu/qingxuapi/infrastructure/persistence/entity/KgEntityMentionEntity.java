package com.qingxu.qingxuapi.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("kg_entity_mention")
public class KgEntityMentionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long entityId;
    private Long documentId;
    private Long chunkId;
    private Long fileId;
    private String mentionText;
    private String titlePath;
    private Integer pageNumber;
    private Integer startOffset;
    private Integer endOffset;
    private BigDecimal confidence;
    private LocalDateTime createdAt;
}
