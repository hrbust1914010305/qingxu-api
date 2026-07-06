package com.qingxu.qingxuapi.application.notification;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qingxu.qingxuapi.common.exception.BusinessException;
import com.qingxu.qingxuapi.common.response.ErrorCode;
import com.qingxu.qingxuapi.common.response.PageResponse;
import com.qingxu.qingxuapi.domain.notification.NotificationReadStatus;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysNotificationEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysNotificationRecipientEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysNotificationMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysNotificationRecipientMapper;
import com.qingxu.qingxuapi.interfaces.notification.dto.NotificationListItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationApplicationService {

    private static final String NOTIFICATION_NOT_FOUND = "通知不存在或无权访问";

    private final SysNotificationMapper notificationMapper;
    private final SysNotificationRecipientMapper recipientMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long createNotification(NotificationCreateCommand command) {
        if (command.recipientUserIds() == null || command.recipientUserIds().isEmpty()) {
            return null;
        }
        SysNotificationEntity notification = SysNotificationEntity.from(command);
        notificationMapper.insert(notification);

        List<SysNotificationRecipientEntity> recipients = new ArrayList<>();
        for (Long userId : command.recipientUserIds()) {
            if (userId == null) {
                continue;
            }
            SysNotificationRecipientEntity recipient = SysNotificationRecipientEntity.unread(notification.getId(), userId);
            recipientMapper.insert(recipient);
            recipients.add(recipient);
        }
        if (!recipients.isEmpty()) {
            eventPublisher.publishEvent(new NotificationCreatedEvent(notification, recipients));
        }
        return notification.getId();
    }

    public long getUnreadCount(Long currentUserId) {
        return recipientMapper.countUnreadByUserId(currentUserId);
    }

    public PageResponse<NotificationListItemResponse> pageNotifications(Long currentUserId, NotificationQuery query) {
        NotificationQuery effectiveQuery = query != null ? query : new NotificationQuery(null, null, null, null);
        Page<NotificationListItemResponse> page = new Page<>(
                effectiveQuery.resolvedCurrent(),
                effectiveQuery.resolvedPageSize()
        );
        String readStatus = effectiveQuery.readStatus() != null ? effectiveQuery.readStatus().name() : null;
        String type = effectiveQuery.type() != null ? effectiveQuery.type().name() : null;
        List<NotificationListItemResponse> records = recipientMapper.selectNotificationPage(
                page,
                currentUserId,
                readStatus,
                type
        );
        return new PageResponse<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional
    public void markAsRead(Long currentUserId, Long recipientId) {
        SysNotificationRecipientEntity recipient = requireOwnedRecipient(currentUserId, recipientId);
        if (NotificationReadStatus.UNREAD.name().equals(recipient.getReadStatus())) {
            recipientMapper.markAsRead(recipientId, LocalDateTime.now());
        }
    }

    @Transactional
    public void markAllAsRead(Long currentUserId) {
        recipientMapper.markAllAsRead(currentUserId, LocalDateTime.now());
    }

    @Transactional
    public void deleteNotification(Long currentUserId, Long recipientId) {
        requireOwnedRecipient(currentUserId, recipientId);
        recipientMapper.softDelete(currentUserId, recipientId);
    }

    private SysNotificationRecipientEntity requireOwnedRecipient(Long currentUserId, Long recipientId) {
        SysNotificationRecipientEntity recipient = recipientMapper.selectById(recipientId);
        if (recipient == null
                || !currentUserId.equals(recipient.getUserId())
                || Boolean.TRUE.equals(recipient.getDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOTIFICATION_NOT_FOUND);
        }
        return recipient;
    }
}
