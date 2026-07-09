package com.qingxu.qingxuapi.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "qingxu.preview")
public class QingxuPreviewProperties {

    private boolean enabled = true;
    private String kkFileViewBaseUrl = "http://127.0.0.1:8012";
    private String publicBaseUrl = "http://127.0.0.1:8081/api";
    private Duration tokenTtl = Duration.ofMinutes(10);
    private LocalService localService = new LocalService();
    private List<String> allowedPreviewExtensions = new ArrayList<>(List.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "md", "json", "xml", "csv",
            "zip", "rar", "7z",
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg",
            "mp3", "mp4"
    ));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKkFileViewBaseUrl() {
        return kkFileViewBaseUrl;
    }

    public void setKkFileViewBaseUrl(String kkFileViewBaseUrl) {
        this.kkFileViewBaseUrl = kkFileViewBaseUrl;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public Duration getTokenTtl() {
        return tokenTtl;
    }

    public void setTokenTtl(Duration tokenTtl) {
        this.tokenTtl = tokenTtl;
    }

    public LocalService getLocalService() {
        return localService;
    }

    public void setLocalService(LocalService localService) {
        this.localService = localService;
    }

    public List<String> getAllowedPreviewExtensions() {
        return allowedPreviewExtensions;
    }

    public void setAllowedPreviewExtensions(List<String> allowedPreviewExtensions) {
        this.allowedPreviewExtensions = allowedPreviewExtensions;
    }

    public static class LocalService {

        private boolean autoStart = false;
        private String javaCommand = "java";
        private String jarPath = "D:/project/qingxu/tools/kkFileView/bin/kkFileView-5.0.0.jar";
        private String workingDirectory = "D:/project/qingxu/tools/kkFileView/bin";
        private String binFolder = "D:/project/qingxu/tools/kkFileView/bin";
        private String trustHost = "127.0.0.1,localhost";
        private String notTrustHost = "default";
        private String logFile = "D:/project/qingxu/tools/kkFileView/log/kkFileView.log";
        private Duration startupTimeout = Duration.ofSeconds(30);

        public boolean isAutoStart() {
            return autoStart;
        }

        public void setAutoStart(boolean autoStart) {
            this.autoStart = autoStart;
        }

        public String getJavaCommand() {
            return javaCommand;
        }

        public void setJavaCommand(String javaCommand) {
            this.javaCommand = javaCommand;
        }

        public String getJarPath() {
            return jarPath;
        }

        public void setJarPath(String jarPath) {
            this.jarPath = jarPath;
        }

        public String getWorkingDirectory() {
            return workingDirectory;
        }

        public void setWorkingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        public String getBinFolder() {
            return binFolder;
        }

        public void setBinFolder(String binFolder) {
            this.binFolder = binFolder;
        }

        public String getTrustHost() {
            return trustHost;
        }

        public void setTrustHost(String trustHost) {
            this.trustHost = trustHost;
        }

        public String getNotTrustHost() {
            return notTrustHost;
        }

        public void setNotTrustHost(String notTrustHost) {
            this.notTrustHost = notTrustHost;
        }

        public String getLogFile() {
            return logFile;
        }

        public void setLogFile(String logFile) {
            this.logFile = logFile;
        }

        public Duration getStartupTimeout() {
            return startupTimeout;
        }

        public void setStartupTimeout(Duration startupTimeout) {
            this.startupTimeout = startupTimeout;
        }
    }
}
