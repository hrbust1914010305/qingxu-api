package com.qingxu.qingxuapi.application.notification;

import com.qingxu.qingxuapi.common.exception.BusinessException;
import com.qingxu.qingxuapi.domain.notification.NotificationLevel;
import com.qingxu.qingxuapi.domain.notification.NotificationReadStatus;
import com.qingxu.qingxuapi.domain.notification.NotificationType;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysNotificationEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysNotificationRecipientEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysNotificationMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysNotificationRecipientMapper;
import com.baomidou.mybatisplus.annotation.TableLogic;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationApplicationServiceTest {

    @Test
    void recipientDeletedUsesBooleanLogicDeleteValues() throws NoSuchFieldException {
        TableLogic tableLogic = SysNotificationRecipientEntity.class
                .getDeclaredField("deleted")
                .getAnnotation(TableLogic.class);

        assertThat(tableLogic).isNotNull();
        assertThat(tableLogic.value()).isEqualTo("false");
        assertThat(tableLogic.delval()).isEqualTo("true");
    }

    @Test
    void createNotificationStartsNewTransactionForAfterCommitPermissionEvents() throws NoSuchMethodException {
        Method method = NotificationApplicationService.class.getMethod(
                "createNotification",
                NotificationCreateCommand.class
        );

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    void createNotificationWritesRecipientsAndPublishesEvent() {
        SysNotificationMapper notificationMapper = mock(SysNotificationMapper.class);
        SysNotificationRecipientMapper recipientMapper = mock(SysNotificationRecipientMapper.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        NotificationApplicationService service = new NotificationApplicationService(
                notificationMapper,
                recipientMapper,
                eventPublisher
        );
        when(notificationMapper.insert(any(SysNotificationEntity.class))).thenAnswer(invocation -> {
            SysNotificationEntity entity = invocation.getArgument(0);
            entity.setId(501L);
            return 1;
        });
        when(recipientMapper.insert(any(SysNotificationRecipientEntity.class))).thenAnswer(invocation -> {
            SysNotificationRecipientEntity entity = invocation.getArgument(0);
            entity.setId(entity.getUserId() + 1000L);
            return 1;
        });

        Long notificationId = service.createNotification(new NotificationCreateCommand(
                NotificationType.PERMISSION,
                NotificationLevel.WARNING,
                "权限已变更",
                "管理员调整了你的角色权限，请刷新页面获取最新权限。",
                "/system/role",
                "ROLE",
                "10",
                1L,
                "admin",
                Set.of(2L, 3L),
                "trace-1"
        ));

        assertThat(notificationId).isEqualTo(501L);
        verify(notificationMapper).insert(any(SysNotificationEntity.class));
        verify(recipientMapper, times(2)).insert(any(SysNotificationRecipientEntity.class));
        verify(eventPublisher).publishEvent(any(NotificationCreatedEvent.class));
    }

    @Test
    void markAsReadRejectsNotificationOwnedByAnotherUser() {
        SysNotificationMapper notificationMapper = mock(SysNotificationMapper.class);
        SysNotificationRecipientMapper recipientMapper = mock(SysNotificationRecipientMapper.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        NotificationApplicationService service = new NotificationApplicationService(
                notificationMapper,
                recipientMapper,
                eventPublisher
        );
        SysNotificationRecipientEntity recipient = new SysNotificationRecipientEntity();
        recipient.setId(1001L);
        recipient.setUserId(9L);
        recipient.setReadStatus(NotificationReadStatus.UNREAD.name());
        recipient.setDeleted(false);
        when(recipientMapper.selectById(1001L)).thenReturn(recipient);

        assertThatThrownBy(() -> service.markAsRead(2L, 1001L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void markAsReadUpdatesOnlyUnreadOwnedNotification() {
        SysNotificationMapper notificationMapper = mock(SysNotificationMapper.class);
        SysNotificationRecipientMapper recipientMapper = mock(SysNotificationRecipientMapper.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        NotificationApplicationService service = new NotificationApplicationService(
                notificationMapper,
                recipientMapper,
                eventPublisher
        );
        SysNotificationRecipientEntity recipient = new SysNotificationRecipientEntity();
        recipient.setId(1001L);
        recipient.setUserId(2L);
        recipient.setReadStatus(NotificationReadStatus.UNREAD.name());
        recipient.setDeleted(false);
        when(recipientMapper.selectById(1001L)).thenReturn(recipient);

        service.markAsRead(2L, 1001L);

        verify(recipientMapper).markAsRead(eq(1001L), any(LocalDateTime.class));
    }
}
