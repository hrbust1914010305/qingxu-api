package com.qingxu.qingxuapi.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_login_log")
public class SysLoginLogEntity {
    @TableId
    private Long id;
    private String eventType;
    private Boolean success;
    private String username;
    private Long userId;
    private String ip;
    private String userAgent;
    private String traceId;
    private LocalDateTime createdAt;
}
