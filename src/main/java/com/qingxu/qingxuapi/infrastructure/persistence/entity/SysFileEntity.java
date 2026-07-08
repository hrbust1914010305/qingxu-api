package com.qingxu.qingxuapi.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_file")
public class SysFileEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String originalName;
    private String storageKey;
    private String storagePath;
    private String mimeType;
    private String extension;
    private Long size;
    private String checksum;
    private String bizType;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
