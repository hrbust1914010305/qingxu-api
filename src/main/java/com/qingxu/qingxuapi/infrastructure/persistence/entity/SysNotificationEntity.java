package com.qingxu.qingxuapi.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qingxu.qingxuapi.application.notification.NotificationCreateCommand;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_notification")
public class SysNotificationEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private String type;
    private String level;
    private String title;
    private String content;
    private String targetUrl;
    private String bizType;
    private String bizId;
    private Long senderId;
    private String senderName;
    private String traceId;
    private LocalDateTime createdAt;

    public static SysNotificationEntity from(NotificationCreateCommand command) {
        SysNotificationEntity entity = new SysNotificationEntity();
        entity.setTenantId("default");
        entity.setType(command.type().name());
        entity.setLevel(command.level().name());
        entity.setTitle(command.title());
        entity.setContent(command.content());
        entity.setTargetUrl(command.targetUrl());
        entity.setBizType(command.bizType());
        entity.setBizId(command.bizId());
        entity.setSenderId(command.senderId());
        entity.setSenderName(command.senderName());
        entity.setTraceId(command.traceId());
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}
