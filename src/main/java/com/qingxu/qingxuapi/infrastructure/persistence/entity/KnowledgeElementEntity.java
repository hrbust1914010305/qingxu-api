package com.qingxu.qingxuapi.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qingxu.qingxuapi.infrastructure.persistence.typehandler.JsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "knowledge_element", autoResultMap = true)
public class KnowledgeElementEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private Long fileId;
    private Integer elementIndex;
    private String elementType;
    private String text;
    private Integer level;
    private String titlePath;
    private Integer pageNumber;
    private String sheetName;
    private Integer slideNumber;
    private Integer startOffset;
    private Integer endOffset;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String metadataJson;
    private LocalDateTime createdAt;
}
