package com.qingxu.qingxuapi.application.notification;

import com.qingxu.qingxuapi.domain.notification.NotificationLevel;
import com.qingxu.qingxuapi.domain.notification.NotificationType;

import java.util.Set;

public record NotificationCreateCommand(
        NotificationType type,
        NotificationLevel level,
        String title,
        String content,
        String targetUrl,
        String bizType,
        String bizId,
        Long senderId,
        String senderName,
        Set<Long> recipientUserIds,
        String traceId
) {
}
