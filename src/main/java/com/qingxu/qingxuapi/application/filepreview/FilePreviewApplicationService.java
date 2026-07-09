package com.qingxu.qingxuapi.application.filepreview;

import com.qingxu.qingxuapi.application.file.FileApplicationService;
import com.qingxu.qingxuapi.application.file.FileDownloadResource;
import com.qingxu.qingxuapi.common.config.QingxuPreviewProperties;
import com.qingxu.qingxuapi.common.exception.BusinessException;
import com.qingxu.qingxuapi.common.response.ErrorCode;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysFileEntity;
import com.qingxu.qingxuapi.interfaces.filepreview.dto.FilePreviewStatusResponse;
import com.qingxu.qingxuapi.interfaces.filepreview.dto.FilePreviewUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FilePreviewApplicationService {

    private final QingxuPreviewProperties previewProperties;
    private final FileApplicationService fileApplicationService;
    private final PreviewTokenService previewTokenService;

    public FilePreviewUrlResponse generatePreviewUrl(Long fileId) {
        ensureEnabled();
        SysFileEntity file = fileApplicationService.getPreviewFile(fileId);
        ensurePreviewable(file.getExtension());
        FilePreviewToken token = previewTokenService.create(fileId);
        String sourceUrl = trimTrailingSlash(previewProperties.getPublicBaseUrl())
                + "/file-preview/source/"
                + token.token()
                + "?fullfilename="
                + URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8);
        String encodedSourceUrl = Base64.getEncoder().encodeToString(sourceUrl.getBytes(StandardCharsets.UTF_8));
        String previewUrl = trimTrailingSlash(previewProperties.getKkFileViewBaseUrl())
                + "/onlinePreview?url="
                + URLEncoder.encode(encodedSourceUrl, StandardCharsets.UTF_8);
        return new FilePreviewUrlResponse(file.getId(), file.getOriginalName(), previewUrl, "READY", token.expiresAt());
    }

    public FilePreviewStatusResponse getStatus(Long fileId) {
        if (!previewProperties.isEnabled()) {
            return new FilePreviewStatusResponse(fileId, "DISABLED", "File preview is disabled");
        }
        SysFileEntity file = fileApplicationService.getPreviewFile(fileId);
        if (!isPreviewable(file.getExtension())) {
            return new FilePreviewStatusResponse(fileId, "UNSUPPORTED", "File preview type is unsupported");
        }
        return new FilePreviewStatusResponse(fileId, "READY", "File can be previewed");
    }

    public FileDownloadResource getSource(String tokenValue) {
        ensureEnabled();
        FilePreviewToken token = previewTokenService.requireValid(tokenValue);
        SysFileEntity file = fileApplicationService.getPreviewFile(token.fileId());
        ensurePreviewable(file.getExtension());
        return fileApplicationService.getPreviewResource(token.fileId());
    }

    private void ensureEnabled() {
        if (!previewProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "File preview is disabled");
        }
    }

    private void ensurePreviewable(String extension) {
        if (!isPreviewable(extension)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "File preview type is unsupported");
        }
    }

    private boolean isPreviewable(String extension) {
        if (extension == null || extension.isBlank()) {
            return false;
        }
        String normalized = extension.toLowerCase(Locale.ROOT);
        return previewProperties.getAllowedPreviewExtensions().stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(normalized));
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
