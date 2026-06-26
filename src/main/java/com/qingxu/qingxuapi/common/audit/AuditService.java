package com.qingxu.qingxuapi.common.audit;

import jakarta.servlet.http.HttpServletRequest;

public interface AuditService {

    void record(AuditEventType eventType, boolean success, String username, Long userId, HttpServletRequest request);
}
