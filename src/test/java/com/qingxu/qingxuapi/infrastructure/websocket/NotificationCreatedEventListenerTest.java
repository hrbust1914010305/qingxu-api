package com.qingxu.qingxuapi.infrastructure.websocket;

import com.qingxu.qingxuapi.application.notification.NotificationCreatedEvent;
import com.qingxu.qingxuapi.common.config.QingxuWebSocketProperties;
import com.qingxu.qingxuapi.domain.notification.NotificationLevel;
import com.qingxu.qingxuapi.domain.notification.NotificationType;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysNotificationEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysNotificationRecipientEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysNotificationRecipientMapper;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationCreatedEventListenerTest {

    @Test
    void handlePushesCreatedPayloadToEachRecipientQueue() {
        SysNotificationRecipientMapper recipientMapper = mock(SysNotificationRecipientMapper.class);
        NotificationWsPusher pusher = mock(NotificationWsPusher.class);
        NotificationCreatedEventListener listener = new NotificationCreatedEventListener(
                recipientMapper,
                pusher
        );
        SysNotificationEntity notification = new SysNotificationEntity();
        notification.setId(501L);
        notification.setType(NotificationType.PERMISSION.name());
        notification.setLevel(NotificationLevel.WARNING.name());
        notification.setTitle("权限已变更");
        notification.setContent("管理员调整了你的角色权限，请刷新页面获取最新权限。");
        notification.setTargetUrl("/system/role");
        notification.setTraceId("trace-1");
        notification.setCreatedAt(LocalDateTime.parse("2026-07-06T10:20:30"));
        SysNotificationRecipientEntity recipient = new SysNotificationRecipientEntity();
        recipient.setId(1001L);
        recipient.setNotificationId(501L);
        recipient.setUserId(2L);
        when(recipientMapper.countUnreadByUserId(2L)).thenReturn(8L);

        listener.handle(new NotificationCreatedEvent(notification, List.of(recipient)));

        verify(pusher).pushToUser(eq(2L), any(NotificationPushPayload.class));
    }

    @Test
    void pusherUsesSpringUserDestinationWithoutUserPrefix() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        NotificationWsPusher pusher = new NotificationWsPusher(messagingTemplate, new QingxuWebSocketProperties());
        NotificationPushPayload payload = new NotificationPushPayload(
                "CREATED",
                1001L,
                501L,
                NotificationType.PERMISSION,
                NotificationLevel.WARNING,
                "权限已变更",
                "管理员调整了你的角色权限，请刷新页面获取最新权限。",
                "/system/role",
                LocalDateTime.parse("2026-07-06T10:20:30"),
                8L,
                "trace-1"
        );

        pusher.pushToUser(2L, payload);

        verify(messagingTemplate).convertAndSendToUser("2", "/queue/notifications", payload);
    }
}
