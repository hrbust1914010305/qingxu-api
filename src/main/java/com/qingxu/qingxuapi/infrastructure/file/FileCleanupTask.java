package com.qingxu.qingxuapi.infrastructure.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingxu.qingxuapi.domain.file.FileStorageService;
import com.qingxu.qingxuapi.domain.file.FileUploadStatus;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysFileUploadChunkEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysFileUploadSessionEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysFileUploadChunkMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysFileUploadSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupTask {

    private final FileStorageService fileStorageService;
    private final SysFileUploadSessionMapper uploadSessionMapper;
    private final SysFileUploadChunkMapper uploadChunkMapper;

    @Scheduled(cron = "${qingxu.file.cleanup-cron}")
    public void cleanupExpiredSessions() {
        List<SysFileUploadSessionEntity> sessions = uploadSessionMapper.selectList(new LambdaQueryWrapper<SysFileUploadSessionEntity>()
                .lt(SysFileUploadSessionEntity::getExpiresAt, LocalDateTime.now())
                .in(SysFileUploadSessionEntity::getStatus, FileUploadStatus.UPLOADING.name(), FileUploadStatus.FAILED.name(), FileUploadStatus.CANCELED.name()));
        for (SysFileUploadSessionEntity session : sessions) {
            cleanupSession(session);
        }
    }

    private void cleanupSession(SysFileUploadSessionEntity session) {
        try {
            fileStorageService.deleteChunks(session.getUploadId());
        } catch (IOException exception) {
            log.warn("Failed to cleanup file chunks: uploadId={}", session.getUploadId(), exception);
            return;
        }
        uploadChunkMapper.delete(new LambdaQueryWrapper<SysFileUploadChunkEntity>()
                .eq(SysFileUploadChunkEntity::getUploadId, session.getUploadId()));
        if (FileUploadStatus.UPLOADING.name().equals(session.getStatus()) || FileUploadStatus.FAILED.name().equals(session.getStatus())) {
            session.setStatus(FileUploadStatus.EXPIRED.name());
            session.setUpdatedAt(LocalDateTime.now());
            uploadSessionMapper.updateById(session);
        }
    }
}
