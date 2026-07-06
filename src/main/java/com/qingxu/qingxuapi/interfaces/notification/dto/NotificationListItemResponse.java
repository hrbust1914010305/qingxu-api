package com.qingxu.qingxuapi.interfaces.notification.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationListItemResponse {
    private Long id;
    private Long notificationId;
    private String type;
    private String level;
    private String title;
    private String content;
    private String targetUrl;
    private String readStatus;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private String traceId;
}
