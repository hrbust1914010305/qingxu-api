package com.qingxu.qingxuapi.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_file_upload_chunk")
public class SysFileUploadChunkEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String uploadId;
    private Integer chunkIndex;
    private Long chunkSize;
    private String checksum;
    private String storagePath;
    private LocalDateTime createdAt;
}
