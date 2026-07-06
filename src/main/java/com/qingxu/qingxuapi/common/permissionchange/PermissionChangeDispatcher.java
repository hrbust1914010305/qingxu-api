package com.qingxu.qingxuapi.common.permissionchange;

import com.qingxu.qingxuapi.common.config.TraceIdFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionChangeDispatcher {

    private final ApplicationEventPublisher eventPublisher;
    private final AffectedUserResolver affectedUserResolver;

    public void fireMenuChange(Long menuId, Long operatorId, String operator, String reason) {
        Set<Long> userIds = affectedUserResolver.resolveByMenu(menuId);
        publish(ChangeType.MENU, menuId, userIds, operatorId, operator, reason, false);
    }

    public void fireRoleChange(Long roleId, Long operatorId, String operator, String reason) {
        Set<Long> userIds = affectedUserResolver.resolveByRole(roleId);
        boolean adminInvolved = affectedUserResolver.isAdminRole(roleId);
        publish(ChangeType.ROLE, roleId, userIds, operatorId, operator, reason, adminInvolved);
    }

    public void fireUserRoleChange(Long userId, Long operatorId, String operator, String reason) {
        Set<Long> userIds = affectedUserResolver.resolveByUser(userId);
        publish(ChangeType.USER_ROLE, userId, userIds, operatorId, operator, reason, false);
    }

    public void fireUserStatusChange(Long userId, Long operatorId, String operator, String reason) {
        Set<Long> userIds = affectedUserResolver.resolveByUserForce(userId);
        publish(ChangeType.USER_STATUS, userId, userIds, operatorId, operator, reason, false);
    }

    public void fireUserDeleteChange(Long userId, Long operatorId, String operator, String reason) {
        Set<Long> userIds = affectedUserResolver.resolveByUserForce(userId);
        publish(ChangeType.USER_DELETE, userId, userIds, operatorId, operator, reason, false);
    }

    private void publish(ChangeType type, Long entityId, Set<Long> userIds,
                         Long operatorId, String operator, String reason, boolean adminInvolved) {
        PermissionChangeEvent event = new PermissionChangeEvent(
                type, entityId, userIds, operator, operatorId, reason,
                LocalDateTime.now(), TraceIdFilter.currentTraceId(), adminInvolved
        );
        eventPublisher.publishEvent(event);
        log.debug("Permission change event published type={} entityId={} affectedUsers={}",
                type, entityId, userIds.size());
    }
}
