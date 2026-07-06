package com.qingxu.qingxuapi.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_permission_change_log")
public class SysPermissionChangeLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String changeType;
    private Long entityId;
    private String affectedUsers;
    private Boolean adminBranch;
    private Long operatorId;
    private String operatorName;
    private String reason;
    private String traceId;
    private LocalDateTime occurredAt;
}