package com.qingxu.qingxuapi.common.permissionchange;

import java.time.LocalDateTime;
import java.util.Set;

public record PermissionChangeEvent(
        ChangeType changeType,
        Long entityId,
        Set<Long> affectedUserIds,
        String operator,
        Long operatorId,
        String reason,
        LocalDateTime occurredAt,
        String traceId,
        boolean adminRoleInvolved
) {
}