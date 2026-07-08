package com.qingxu.qingxuapi.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_file_upload_session")
public class SysFileUploadSessionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String uploadId;
    private String fingerprint;
    private String originalName;
    private String mimeType;
    private String extension;
    private Long totalSize;
    private Long chunkSize;
    private Integer totalChunks;
    private String bizType;
    private String status;
    private LocalDateTime expiresAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
