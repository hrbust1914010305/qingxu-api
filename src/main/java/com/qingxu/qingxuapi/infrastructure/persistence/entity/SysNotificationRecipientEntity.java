package com.qingxu.qingxuapi.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qingxu.qingxuapi.domain.notification.NotificationReadStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_notification_recipient")
public class SysNotificationRecipientEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long notificationId;
    private Long userId;
    private String readStatus;
    private LocalDateTime readAt;
    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;
    private LocalDateTime createdAt;

    public static SysNotificationRecipientEntity unread(Long notificationId, Long userId) {
        SysNotificationRecipientEntity entity = new SysNotificationRecipientEntity();
        entity.setNotificationId(notificationId);
        entity.setUserId(userId);
        entity.setReadStatus(NotificationReadStatus.UNREAD.name());
        entity.setDeleted(false);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}
