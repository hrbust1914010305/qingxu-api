package com.qingxu.qingxuapi.common.permissionchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingxu.qingxuapi.application.notification.NotificationApplicationService;
import com.qingxu.qingxuapi.application.notification.NotificationCreateCommand;
import com.qingxu.qingxuapi.common.config.QingxuWebSocketProperties;
import com.qingxu.qingxuapi.domain.notification.NotificationType;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysPermissionChangeLogEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysPermissionChangeLogMapper;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermissionChangeEventListenerTest {

    @Test
    void handlePushesStompMessageBeforeInvalidatingSessionsForRegularRoleChange() {
        @SuppressWarnings("unchecked")
        FindByIndexNameSessionRepository<Session> sessionRepository = mock(FindByIndexNameSessionRepository.class);
        SysPermissionChangeLogMapper auditMapper = mock(SysPermissionChangeLogMapper.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider = mock(ObjectProvider.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        NotificationApplicationService notificationService = mock(NotificationApplicationService.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        PermissionChangeProperties properties = new PermissionChangeProperties();
        Session session = mock(Session.class);
        Map<String, Session> sessions = new LinkedHashMap<>();
        sessions.put("session-1", session);

        when(messagingTemplateProvider.getIfAvailable()).thenReturn(messagingTemplate);
        when(sessionRepository.findByPrincipalName("2")).thenReturn(sessions);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        PermissionChangeEventListener listener = new PermissionChangeEventListener(
                sessionRepository,
                auditMapper,
                new ObjectMapper(),
                messagingTemplateProvider,
                redisTemplate,
                properties,
                notificationService,
                new QingxuWebSocketProperties()
        );
        PermissionChangeEvent event = new PermissionChangeEvent(
                ChangeType.ROLE,
                10L,
                Set.of(2L),
                "admin",
                1L,
                "update ui role",
                LocalDateTime.parse("2026-07-06T12:00:00"),
                "trace-1",
                false
        );

        listener.handle(event);

        var ordered = inOrder(messagingTemplate, sessionRepository);
        ordered.verify(messagingTemplate).convertAndSendToUser(
                eq("2"),
                eq("/queue/permission-reload"),
                any(PermissionReloadPayload.class)
        );
        ordered.verify(sessionRepository).deleteById("session-1");
    }

    @Test
    void handleStillPushesStompMessageWhenAuditIdempotentKeyExists() {
        @SuppressWarnings("unchecked")
        FindByIndexNameSessionRepository<Session> sessionRepository = mock(FindByIndexNameSessionRepository.class);
        SysPermissionChangeLogMapper auditMapper = mock(SysPermissionChangeLogMapper.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider = mock(ObjectProvider.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        NotificationApplicationService notificationService = mock(NotificationApplicationService.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        when(messagingTemplateProvider.getIfAvailable()).thenReturn(messagingTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        PermissionChangeEventListener listener = new PermissionChangeEventListener(
                sessionRepository,
                auditMapper,
                new ObjectMapper(),
                messagingTemplateProvider,
                redisTemplate,
                new PermissionChangeProperties(),
                notificationService,
                new QingxuWebSocketProperties()
        );
        PermissionChangeEvent event = new PermissionChangeEvent(
                ChangeType.ROLE,
                10L,
                Set.of(2L),
                "admin",
                1L,
                "update ui role",
                LocalDateTime.parse("2026-07-06T12:00:00"),
                "trace-1",
                false
        );

        listener.handle(event);

        verify(messagingTemplate).convertAndSendToUser(
                eq("2"),
                eq("/queue/permission-reload"),
                any(PermissionReloadPayload.class)
        );
        verify(auditMapper, never()).insert(any(SysPermissionChangeLogEntity.class));
    }

    @Test
    void handleCreatesPermissionNotificationHistoryForAffectedUsers() {
        @SuppressWarnings("unchecked")
        FindByIndexNameSessionRepository<Session> sessionRepository = mock(FindByIndexNameSessionRepository.class);
        SysPermissionChangeLogMapper auditMapper = mock(SysPermissionChangeLogMapper.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider = mock(ObjectProvider.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        NotificationApplicationService notificationService = mock(NotificationApplicationService.class);

        when(messagingTemplateProvider.getIfAvailable()).thenReturn(messagingTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        PermissionChangeEventListener listener = new PermissionChangeEventListener(
                sessionRepository,
                auditMapper,
                new ObjectMapper(),
                messagingTemplateProvider,
                redisTemplate,
                new PermissionChangeProperties(),
                notificationService,
                new QingxuWebSocketProperties()
        );
        PermissionChangeEvent event = new PermissionChangeEvent(
                ChangeType.USER_ROLE,
                2L,
                Set.of(2L),
                "admin",
                1L,
                "用户角色分配变更",
                LocalDateTime.parse("2026-07-06T12:00:00"),
                "trace-1",
                false
        );

        listener.handle(event);

        ArgumentCaptor<NotificationCreateCommand> captor = ArgumentCaptor.forClass(NotificationCreateCommand.class);
        verify(notificationService).createNotification(captor.capture());
        NotificationCreateCommand command = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(command.type()).isEqualTo(NotificationType.PERMISSION);
        org.assertj.core.api.Assertions.assertThat(command.recipientUserIds()).containsExactly(2L);
        org.assertj.core.api.Assertions.assertThat(command.traceId()).isEqualTo("trace-1");
    }
}
