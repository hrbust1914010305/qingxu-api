package com.qingxu.qingxuapi.application.notification;

import com.qingxu.qingxuapi.domain.notification.NotificationReadStatus;
import com.qingxu.qingxuapi.domain.notification.NotificationType;

public record NotificationQuery(
        Integer current,
        Integer pageSize,
        NotificationReadStatus readStatus,
        NotificationType type
) {
    public int resolvedCurrent() {
        return current == null || current < 1 ? 1 : current;
    }

    public int resolvedPageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }
}
