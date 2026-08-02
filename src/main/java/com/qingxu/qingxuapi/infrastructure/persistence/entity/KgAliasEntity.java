package com.qingxu.qingxuapi.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("kg_alias")
public class KgAliasEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long entityId;
    private String alias;
    private String source;
    private BigDecimal confidence;
    private LocalDateTime createdAt;
}
