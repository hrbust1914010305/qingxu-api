package com.qingxu.qingxuapi.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("kg_relation_evidence")
public class KgRelationEvidenceEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long relationId;
    private Long documentId;
    private Long chunkId;
    private String evidenceText;
    private BigDecimal confidence;
    private LocalDateTime createdAt;
}
