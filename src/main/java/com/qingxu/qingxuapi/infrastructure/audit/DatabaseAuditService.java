package com.qingxu.qingxuapi.infrastructure.audit;

import com.qingxu.qingxuapi.common.audit.AuditEventType;
import com.qingxu.qingxuapi.common.audit.AuditService;
import com.qingxu.qingxuapi.common.config.TraceIdFilter;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysLoginLogEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysLoginLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseAuditService implements AuditService {

    private final SysLoginLogMapper sysLoginLogMapper;

    @Override
    public void record(AuditEventType eventType, boolean success, String username, Long userId, HttpServletRequest request) {
        String ip = request == null ? null : request.getRemoteAddr();
        String userAgent = request == null ? null : request.getHeader("User-Agent");

        SysLoginLogEntity logEntity = new SysLoginLogEntity();
        logEntity.setEventType(eventType.name());
        logEntity.setSuccess(success);
        logEntity.setUsername(username);
        logEntity.setUserId(userId);
        logEntity.setIp(ip);
        logEntity.setUserAgent(userAgent);
        logEntity.setTraceId(TraceIdFilter.currentTraceId());
        logEntity.setCreatedAt(LocalDateTime.now());

        try {
            sysLoginLogMapper.insert(logEntity);
            log.info("audit recorded: eventType={} success={} username={}", eventType, success, username);
        } catch (Exception e) {
            log.error("failed to record audit log: eventType={} error={}", eventType, e.getMessage(), e);
        }
    }
}