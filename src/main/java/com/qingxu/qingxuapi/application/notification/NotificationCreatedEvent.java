package com.qingxu.qingxuapi.application.notification;

import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysNotificationEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysNotificationRecipientEntity;

import java.util.List;

public record NotificationCreatedEvent(
        SysNotificationEntity notification,
        List<SysNotificationRecipientEntity> recipients
) {
}
