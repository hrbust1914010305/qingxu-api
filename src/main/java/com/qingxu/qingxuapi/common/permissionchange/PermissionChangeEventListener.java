package com.qingxu.qingxuapi.common.permissionchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingxu.qingxuapi.application.notification.NotificationApplicationService;
import com.qingxu.qingxuapi.application.notification.NotificationCreateCommand;
import com.qingxu.qingxuapi.domain.notification.NotificationLevel;
import com.qingxu.qingxuapi.domain.notification.NotificationType;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysPermissionChangeLogEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysPermissionChangeLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 权限变更事件监听：事务提交后执行。
 * <p>
 * - 普通分支：失效该用户的所有 RedisIndexedSession + 推送 STOMP
 * - admin 分支：不失效 Session + 推送 STOMP（requireReLogin=false）
 * - 审计幂等由 Dispatcher 通过 Redis key 控制（同一 (changeType, entityId) 24h 内只发布一次事件）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionChangeEventListener {

    private static final String QUEUE_DESTINATION = "/queue/permission-reload";
    private static final String IDEMPOTENT_KEY_PREFIX = "perm-change:";

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;
    private final SysPermissionChangeLogMapper auditMapper;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;
    private final StringRedisTemplate redisTemplate;
    private final PermissionChangeProperties properties;
    private final NotificationApplicationService notificationApplicationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PermissionChangeEvent event) {
        boolean adminBranch = event.adminRoleInvolved() && event.changeType() == ChangeType.ROLE;
        Set<Long> userIds = event.affectedUserIds();

        for (Long userId : userIds) {
            String userKey = String.valueOf(userId);

            pushStompMessage(userKey, event, adminBranch);

            if (!adminBranch) {
                invalidateSessions(userKey);
            }
        }

        createPermissionNotification(event);
        recordAudit(event, adminBranch);
    }

    private void createPermissionNotification(PermissionChangeEvent event) {
        if (event.affectedUserIds() == null || event.affectedUserIds().isEmpty()) {
            return;
        }
        try {
            notificationApplicationService.createNotification(new NotificationCreateCommand(
                    NotificationType.PERMISSION,
                    NotificationLevel.WARNING,
                    "权限已变更",
                    event.reason() != null ? event.reason() : "你的权限发生变化，请刷新页面或重新登录后继续使用。",
                    "/system/role",
                    event.changeType().name(),
                    String.valueOf(event.entityId()),
                    event.operatorId(),
                    event.operator(),
                    event.affectedUserIds(),
                    event.traceId()
            ));
        } catch (Exception ex) {
            log.warn("权限变更通知中心写入失败: type={} entityId={} reason={}",
                    event.changeType(),
                    event.entityId(),
                    ex.getMessage()
            );
        }
    }

    private void invalidateSessions(String userKey) {
        try {
            Map<String, ? extends Session> sessions = sessionRepository.findByPrincipalName(userKey);
            if (sessions == null || sessions.isEmpty()) {
                return;
            }
            for (String sessionId : sessions.keySet()) {
                sessionRepository.deleteById(sessionId);
                log.info("已失效 session={} user={}", sessionId, userKey);
            }
        } catch (Exception e) {
            log.warn("失效用户[{}]的 Session 失败: {}", userKey, e.getMessage());
        }
    }

    private void pushStompMessage(String userKey, PermissionChangeEvent event, boolean adminBranch) {
        SimpMessagingTemplate template = messagingTemplateProvider.getIfAvailable();
        if (template == null) {
            log.debug("SimpMessagingTemplate 尚未就绪，跳过 STOMP 推送 user={}", userKey);
            return;
        }
        PermissionReloadPayload payload = PermissionReloadPayload.of(
                event.changeType().name(),
                event.reason(),
                event.occurredAt().toString(),
                !adminBranch,
                event.traceId()
        );
        template.convertAndSendToUser(userKey, QUEUE_DESTINATION, payload);
        log.debug("STOMP 推送成功 user={} requireReLogin={}", userKey, !adminBranch);
    }

    private void recordAudit(PermissionChangeEvent event, boolean adminBranch) {
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    IDEMPOTENT_KEY_PREFIX + event.changeType() + ":" + event.entityId(),
                    "1",
                    properties.getIdempotentWindow()
            );
            if (Boolean.FALSE.equals(acquired)) {
                log.debug("Permission change audit skipped by idempotency type={} entityId={}",
                        event.changeType(), event.entityId());
                return;
            }
            SysPermissionChangeLogEntity entity = new SysPermissionChangeLogEntity();
            entity.setChangeType(event.changeType().name());
            entity.setEntityId(event.entityId());
            entity.setAffectedUsers(objectMapper.writeValueAsString(List.copyOf(event.affectedUserIds())));
            entity.setAdminBranch(adminBranch);
            entity.setOperatorId(event.operatorId());
            entity.setOperatorName(event.operator());
            entity.setReason(event.reason());
            entity.setTraceId(event.traceId());
            entity.setOccurredAt(event.occurredAt());
            auditMapper.insert(entity);
        } catch (Exception e) {
            log.warn("审计落库失败: {}", e.getMessage());
        }
    }
}
