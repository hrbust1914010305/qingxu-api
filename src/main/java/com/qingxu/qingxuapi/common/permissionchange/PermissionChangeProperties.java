package com.qingxu.qingxuapi.common.permissionchange;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "qingxu.permission-change")
public class PermissionChangeProperties {

    private Duration idempotentWindow = Duration.ofSeconds(10);
    private boolean skipAdminKickout = true;

    public Duration getIdempotentWindow() {
        return idempotentWindow;
    }

    public void setIdempotentWindow(Duration idempotentWindow) {
        this.idempotentWindow = idempotentWindow;
    }

    public boolean isSkipAdminKickout() {
        return skipAdminKickout;
    }

    public void setSkipAdminKickout(boolean skipAdminKickout) {
        this.skipAdminKickout = skipAdminKickout;
    }
}