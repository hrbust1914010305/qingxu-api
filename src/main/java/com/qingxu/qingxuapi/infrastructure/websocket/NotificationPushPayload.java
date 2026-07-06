package com.qingxu.qingxuapi.infrastructure.websocket;

import com.qingxu.qingxuapi.domain.notification.NotificationLevel;
import com.qingxu.qingxuapi.domain.notification.NotificationType;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysNotificationEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysNotificationRecipientEntity;

import java.time.LocalDateTime;

public record NotificationPushPayload(
        String eventType,
        Long recipientId,
        Long notificationId,
        NotificationType type,
        NotificationLevel level,
        String title,
        String content,
        String targetUrl,
        LocalDateTime createdAt,
        long unreadCount,
        String traceId
) {
    public static NotificationPushPayload created(
            SysNotificationRecipientEntity recipient,
            SysNotificationEntity notification,
            long unreadCount
    ) {
        return new NotificationPushPayload(
                "CREATED",
                recipient.getId(),
                notification.getId(),
                NotificationType.valueOf(notification.getType()),
                NotificationLevel.valueOf(notification.getLevel()),
                notification.getTitle(),
                notification.getContent(),
                notification.getTargetUrl(),
                notification.getCreatedAt(),
                unreadCount,
                notification.getTraceId()
        );
    }
}
