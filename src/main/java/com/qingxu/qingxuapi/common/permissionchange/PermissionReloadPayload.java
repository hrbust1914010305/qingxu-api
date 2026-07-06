package com.qingxu.qingxuapi.common.permissionchange;

public record PermissionReloadPayload(
        String changeType,
        String reason,
        String occurredAt,
        boolean requireReLogin,
        String traceId
) {
    public static PermissionReloadPayload of(String changeType,
                                             String reason,
                                             String occurredAt,
                                             boolean requireReLogin,
                                             String traceId) {
        return new PermissionReloadPayload(changeType, reason, occurredAt, requireReLogin, traceId);
    }
}