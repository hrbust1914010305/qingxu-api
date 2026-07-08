package com.qingxu.qingxuapi.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "qingxu.websocket")
public class QingxuWebSocketProperties {

    private String endpoint = "/ws";
    private List<String> allowedOriginPatterns = new ArrayList<>();
    private List<String> brokerPrefixes = List.of("/queue", "/topic");
    private String applicationDestinationPrefix = "/app";
    private String userDestinationPrefix = "/user";
    private long[] heartbeat = new long[]{10_000, 10_000};
    private int heartbeatPoolSize = 2;
    private String notificationQueue = "/queue/notifications";
    private String permissionReloadQueue = "/queue/permission-reload";

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public List<String> getAllowedOriginPatterns() {
        return allowedOriginPatterns;
    }

    public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    public List<String> getBrokerPrefixes() {
        return brokerPrefixes;
    }

    public void setBrokerPrefixes(List<String> brokerPrefixes) {
        this.brokerPrefixes = brokerPrefixes;
    }

    public String getApplicationDestinationPrefix() {
        return applicationDestinationPrefix;
    }

    public void setApplicationDestinationPrefix(String applicationDestinationPrefix) {
        this.applicationDestinationPrefix = applicationDestinationPrefix;
    }

    public String getUserDestinationPrefix() {
        return userDestinationPrefix;
    }

    public void setUserDestinationPrefix(String userDestinationPrefix) {
        this.userDestinationPrefix = userDestinationPrefix;
    }

    public long[] getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(long[] heartbeat) {
        this.heartbeat = heartbeat;
    }

    public int getHeartbeatPoolSize() {
        return heartbeatPoolSize;
    }

    public void setHeartbeatPoolSize(int heartbeatPoolSize) {
        this.heartbeatPoolSize = heartbeatPoolSize;
    }

    public String getNotificationQueue() {
        return notificationQueue;
    }

    public void setNotificationQueue(String notificationQueue) {
        this.notificationQueue = notificationQueue;
    }

    public String getPermissionReloadQueue() {
        return permissionReloadQueue;
    }

    public void setPermissionReloadQueue(String permissionReloadQueue) {
        this.permissionReloadQueue = permissionReloadQueue;
    }
}
