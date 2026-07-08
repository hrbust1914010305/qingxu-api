package com.qingxu.qingxuapi.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "qingxu.session")
public class QingxuSessionProperties {

    private Duration timeout = Duration.ofHours(2);
    private String redisNamespace = "spring:session:qingxu";
    private final Cookie cookie = new Cookie();

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public String getRedisNamespace() {
        return redisNamespace;
    }

    public void setRedisNamespace(String redisNamespace) {
        this.redisNamespace = redisNamespace;
    }

    public Cookie getCookie() {
        return cookie;
    }

    public static class Cookie {
        private String name = "QINGXU_SESSION";
        private String path = "/";
        private String domain;
        private int maxAge = -1;
        private boolean httpOnly = true;
        private SameSite sameSite = new SameSite();
        private Secure secure = new Secure();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public int getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(int maxAge) {
            this.maxAge = maxAge;
        }

        public boolean isHttpOnly() {
            return httpOnly;
        }

        public void setHttpOnly(boolean httpOnly) {
            this.httpOnly = httpOnly;
        }

        public SameSite getSameSite() {
            return sameSite;
        }

        public Secure getSecure() {
            return secure;
        }
    }

    public static class SameSite {
        private String dev = "Lax";
        private String prod = "None";

        public String getDev() {
            return dev;
        }

        public void setDev(String dev) {
            this.dev = dev;
        }

        public String getProd() {
            return prod;
        }

        public void setProd(String prod) {
            this.prod = prod;
        }
    }

    public static class Secure {
        private boolean dev;
        private boolean prod = true;

        public boolean isDev() {
            return dev;
        }

        public void setDev(boolean dev) {
            this.dev = dev;
        }

        public boolean isProd() {
            return prod;
        }

        public void setProd(boolean prod) {
            this.prod = prod;
        }
    }
}
