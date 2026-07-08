package com.qingxu.qingxuapi.infrastructure.websocket;

import com.qingxu.qingxuapi.common.config.QingxuWebSocketProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWsPusher {

    private final SimpMessagingTemplate messagingTemplate;
    private final QingxuWebSocketProperties webSocketProperties;

    public void pushToUser(Long userId, NotificationPushPayload payload) {
        try {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    webSocketProperties.getNotificationQueue(),
                    payload
            );
        } catch (Exception ex) {
            log.warn("[notification] WebSocket push failed, userId={}, notificationId={}",
                    userId,
                    payload.notificationId(),
                    ex
            );
        }
    }
}
