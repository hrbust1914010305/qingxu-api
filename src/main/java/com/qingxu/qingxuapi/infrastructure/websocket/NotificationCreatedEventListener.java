package com.qingxu.qingxuapi.infrastructure.websocket;

import com.qingxu.qingxuapi.application.notification.NotificationCreatedEvent;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysNotificationRecipientEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysNotificationRecipientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCreatedEventListener {

    private final SysNotificationRecipientMapper recipientMapper;
    private final NotificationWsPusher notificationWsPusher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(NotificationCreatedEvent event) {
        for (SysNotificationRecipientEntity recipient : event.recipients()) {
            try {
                long unreadCount = recipientMapper.countUnreadByUserId(recipient.getUserId());
                NotificationPushPayload payload = NotificationPushPayload.created(
                        recipient,
                        event.notification(),
                        unreadCount
                );
                notificationWsPusher.pushToUser(recipient.getUserId(), payload);
            } catch (Exception ex) {
                log.warn("[notification] created event push failed, userId={}, notificationId={}",
                        recipient.getUserId(),
                        event.notification().getId(),
                        ex
                );
            }
        }
    }
}
