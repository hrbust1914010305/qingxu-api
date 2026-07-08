package com.qingxu.qingxuapi.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "qingxu.file")
public class QingxuFileProperties {

    private String storageType = "local";
    private String localRoot = "./data/files";
    private String chunkRoot = "./data/file-chunks";
    private DataSize normalUploadThreshold = DataSize.ofMegabytes(20);
    private DataSize chunkSize = DataSize.ofMegabytes(5);
    private DataSize maxSize = DataSize.ofGigabytes(2);
    private Duration uploadSessionTtl = Duration.ofHours(24);
    private String cleanupCron = "0 0 * * * ?";
    private List<String> allowedExtensions = new ArrayList<>(List.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "png", "jpg", "jpeg", "gif", "zip", "rar", "7z",
            "txt", "mp3", "mp4"
    ));
    private List<String> allowedBizTypes = new ArrayList<>(List.of("default", "knowledge"));
    private String downloadDisposition = "attachment";
    private Checksum checksum = new Checksum();

    @PostConstruct
    void validate() {
        if (chunkSize.toBytes() <= 0 || normalUploadThreshold.toBytes() <= 0 || maxSize.toBytes() <= 0) {
            throw new IllegalStateException("File size settings must be positive");
        }
        if (maxSize.toBytes() < normalUploadThreshold.toBytes()) {
            throw new IllegalStateException("qingxu.file.max-size must be greater than or equal to normal-upload-threshold");
        }
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getLocalRoot() {
        return localRoot;
    }

    public void setLocalRoot(String localRoot) {
        this.localRoot = localRoot;
    }

    public String getChunkRoot() {
        return chunkRoot;
    }

    public void setChunkRoot(String chunkRoot) {
        this.chunkRoot = chunkRoot;
    }

    public DataSize getNormalUploadThreshold() {
        return normalUploadThreshold;
    }

    public void setNormalUploadThreshold(DataSize normalUploadThreshold) {
        this.normalUploadThreshold = normalUploadThreshold;
    }

    public DataSize getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(DataSize chunkSize) {
        this.chunkSize = chunkSize;
    }

    public DataSize getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(DataSize maxSize) {
        this.maxSize = maxSize;
    }

    public Duration getUploadSessionTtl() {
        return uploadSessionTtl;
    }

    public void setUploadSessionTtl(Duration uploadSessionTtl) {
        this.uploadSessionTtl = uploadSessionTtl;
    }

    public String getCleanupCron() {
        return cleanupCron;
    }

    public void setCleanupCron(String cleanupCron) {
        this.cleanupCron = cleanupCron;
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }

    public List<String> getAllowedBizTypes() {
        return allowedBizTypes;
    }

    public void setAllowedBizTypes(List<String> allowedBizTypes) {
        this.allowedBizTypes = allowedBizTypes;
    }

    public String getDownloadDisposition() {
        return downloadDisposition;
    }

    public void setDownloadDisposition(String downloadDisposition) {
        this.downloadDisposition = downloadDisposition;
    }

    public Checksum getChecksum() {
        return checksum;
    }

    public void setChecksum(Checksum checksum) {
        this.checksum = checksum;
    }

    public static class Checksum {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
