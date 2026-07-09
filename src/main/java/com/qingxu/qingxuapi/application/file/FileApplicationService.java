package com.qingxu.qingxuapi.application.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qingxu.qingxuapi.common.config.QingxuFileProperties;
import com.qingxu.qingxuapi.common.exception.BusinessException;
import com.qingxu.qingxuapi.common.response.ErrorCode;
import com.qingxu.qingxuapi.domain.file.FileStorageObject;
import com.qingxu.qingxuapi.domain.file.FileStorageService;
import com.qingxu.qingxuapi.domain.file.FileUploadStatus;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysFileEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysFileUploadChunkEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysFileUploadSessionEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysFileMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysFileUploadChunkMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysFileUploadSessionMapper;
import com.qingxu.qingxuapi.interfaces.auth.dto.CurrentUserResponse;
import com.qingxu.qingxuapi.interfaces.file.dto.FileUploadCompleteRequest;
import com.qingxu.qingxuapi.interfaces.file.dto.FileUploadInitRequest;
import com.qingxu.qingxuapi.interfaces.file.dto.FileUploadInitResponse;
import com.qingxu.qingxuapi.interfaces.file.dto.FileUploadStatusResponse;
import com.qingxu.qingxuapi.interfaces.file.dto.FileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileApplicationService {

    private static final ZoneOffset ZONE_OFFSET = ZoneOffset.ofHours(8);
    private static final String DEFAULT_BIZ_TYPE = "default";

    private final QingxuFileProperties fileProperties;
    private final FileStorageService fileStorageService;
    private final SysFileMapper fileMapper;
    private final SysFileUploadSessionMapper uploadSessionMapper;
    private final SysFileUploadChunkMapper uploadChunkMapper;

    @Transactional
    public FileVO upload(MultipartFile file, String bizType, CurrentUserResponse currentUser) {
        validateFile(file);
        String normalizedBizType = normalizeBizType(bizType);
        String extension = validateExtension(file.getOriginalFilename());
        try {
            FileStorageObject storageObject = fileStorageService.store(file, normalizedBizType, extension);
            SysFileEntity entity = buildFileEntity(file.getOriginalFilename(), file.getContentType(), normalizedBizType, currentUser.id(), storageObject);
            fileMapper.insert(entity);
            return toVO(entity);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件保存失败");
        }
    }

    @Transactional
    public FileUploadInitResponse initMultipart(FileUploadInitRequest request, CurrentUserResponse currentUser) {
        validateSize(request.size());
        String normalizedBizType = normalizeBizType(request.bizType());
        String extension = validateExtension(request.fileName());
        long chunkSize = request.chunkSize() == null ? fileProperties.getChunkSize().toBytes() : request.chunkSize();
        if (chunkSize <= 0) {
            throw new BusinessException(ErrorCode.FILE_CHUNK_SIZE_INVALID);
        }
        int totalChunks = (int) Math.ceil((double) request.size() / chunkSize);
        LocalDateTime now = LocalDateTime.now();

        SysFileUploadSessionEntity existing = uploadSessionMapper.selectOne(new LambdaQueryWrapper<SysFileUploadSessionEntity>()
                .eq(SysFileUploadSessionEntity::getFingerprint, request.fingerprint())
                .eq(SysFileUploadSessionEntity::getCreatedBy, currentUser.id())
                .eq(SysFileUploadSessionEntity::getStatus, FileUploadStatus.UPLOADING.name())
                .gt(SysFileUploadSessionEntity::getExpiresAt, now)
                .last("limit 1"));
        if (existing != null) {
            return toInitResponse(existing, uploadedChunks(existing.getUploadId()));
        }

        SysFileUploadSessionEntity session = new SysFileUploadSessionEntity();
        session.setUploadId("u_" + UUID.randomUUID().toString().replace("-", ""));
        session.setFingerprint(request.fingerprint());
        session.setOriginalName(safeOriginalName(request.fileName()));
        session.setMimeType(request.mimeType());
        session.setExtension(extension);
        session.setTotalSize(request.size());
        session.setChunkSize(chunkSize);
        session.setTotalChunks(totalChunks);
        session.setBizType(normalizedBizType);
        session.setStatus(FileUploadStatus.UPLOADING.name());
        session.setExpiresAt(now.plus(fileProperties.getUploadSessionTtl()));
        session.setCreatedBy(currentUser.id());
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        uploadSessionMapper.insert(session);
        return toInitResponse(session, List.of());
    }

    public FileUploadStatusResponse getMultipartStatus(String uploadId, CurrentUserResponse currentUser) {
        SysFileUploadSessionEntity session = loadSession(uploadId, currentUser);
        return new FileUploadStatusResponse(
                session.getUploadId(),
                session.getStatus(),
                session.getChunkSize(),
                session.getTotalChunks(),
                uploadedChunks(uploadId),
                toOffset(session.getExpiresAt())
        );
    }

    @Transactional
    public void uploadChunk(String uploadId, Integer chunkIndex, MultipartFile chunk, String checksum, CurrentUserResponse currentUser) {
        SysFileUploadSessionEntity session = loadActiveSession(uploadId, currentUser);
        if (chunkIndex == null || chunkIndex < 0 || chunkIndex >= session.getTotalChunks()) {
            throw new BusinessException(ErrorCode.FILE_CHUNK_INDEX_INVALID);
        }
        long expectedSize = expectedChunkSize(session, chunkIndex);
        if (chunk == null || chunk.isEmpty() || chunk.getSize() != expectedSize) {
            throw new BusinessException(ErrorCode.FILE_CHUNK_SIZE_INVALID);
        }
        try {
            String storagePath = fileStorageService.storeChunk(uploadId, chunkIndex, chunk);
            SysFileUploadChunkEntity entity = uploadChunkMapper.selectOne(new LambdaQueryWrapper<SysFileUploadChunkEntity>()
                    .eq(SysFileUploadChunkEntity::getUploadId, uploadId)
                    .eq(SysFileUploadChunkEntity::getChunkIndex, chunkIndex));
            if (entity == null) {
                entity = new SysFileUploadChunkEntity();
                entity.setUploadId(uploadId);
                entity.setChunkIndex(chunkIndex);
                entity.setCreatedAt(LocalDateTime.now());
            }
            entity.setChunkSize(chunk.getSize());
            entity.setChecksum(checksum);
            entity.setStoragePath(storagePath);
            if (entity.getId() == null) {
                try {
                    uploadChunkMapper.insert(entity);
                } catch (DuplicateKeyException exception) {
                    updateChunk(uploadId, chunkIndex, chunk, checksum, storagePath);
                }
            } else {
                uploadChunkMapper.updateById(entity);
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "分片保存失败");
        }
    }

    @Transactional
    public FileVO completeMultipart(String uploadId, FileUploadCompleteRequest request, CurrentUserResponse currentUser) {
        SysFileUploadSessionEntity session = loadActiveSession(uploadId, currentUser);
        if (uploadedChunks(uploadId).size() != session.getTotalChunks()) {
            throw new BusinessException(ErrorCode.FILE_CHUNK_MISSING);
        }
        int changed = uploadSessionMapper.updateStatusIfCurrent(uploadId, FileUploadStatus.UPLOADING.name(), FileUploadStatus.COMPLETING.name());
        if (changed != 1) {
            throw new BusinessException(ErrorCode.FILE_MERGE_FAILED, "上传会话正在合并或已结束");
        }
        try {
            FileStorageObject storageObject = fileStorageService.mergeChunks(uploadId, session.getTotalChunks(), session.getBizType(), session.getExtension());
            if (request != null && request.checksum() != null && !request.checksum().isBlank() && !request.checksum().equalsIgnoreCase(storageObject.checksum())) {
                throw new BusinessException(ErrorCode.FILE_MERGE_FAILED, "文件校验失败");
            }
            SysFileEntity file = buildFileEntity(session.getOriginalName(), session.getMimeType(), session.getBizType(), currentUser.id(), storageObject);
            fileMapper.insert(file);
            markSession(uploadId, FileUploadStatus.COMPLETED);
            deleteChunkRecords(uploadId);
            fileStorageService.deleteChunks(uploadId);
            return toVO(file);
        } catch (BusinessException exception) {
            markSession(uploadId, FileUploadStatus.FAILED);
            throw exception;
        } catch (IOException exception) {
            markSession(uploadId, FileUploadStatus.FAILED);
            throw new BusinessException(ErrorCode.FILE_MERGE_FAILED);
        }
    }

    @Transactional
    public void cancelMultipart(String uploadId, CurrentUserResponse currentUser) {
        SysFileUploadSessionEntity session = loadSession(uploadId, currentUser);
        if (FileUploadStatus.COMPLETED.name().equals(session.getStatus())) {
            return;
        }
        markSession(uploadId, FileUploadStatus.CANCELED);
        deleteChunkRecords(uploadId);
        try {
            fileStorageService.deleteChunks(uploadId);
        } catch (IOException exception) {
            log.warn("Failed to delete canceled file chunks: uploadId={}", uploadId, exception);
        }
    }

    @Transactional
    public void deleteFile(Long fileId, CurrentUserResponse currentUser) {
        SysFileEntity file = loadOwnedFile(fileId, currentUser);
        fileMapper.deleteById(fileId);
        try {
            fileStorageService.delete(file.getStoragePath());
        } catch (IOException exception) {
            log.warn("Failed to delete physical file: fileId={}", fileId, exception);
        }
    }

    public FileDownloadResource getDownloadResource(Long fileId, CurrentUserResponse currentUser) {
        SysFileEntity file = loadOwnedFile(fileId, currentUser);
        try {
            return new FileDownloadResource(file.getOriginalName(), mimeType(file.getMimeType()), file.getSize(), fileStorageService.load(file.getStoragePath()));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    public FileDownloadResource getPreviewResource(Long fileId) {
        SysFileEntity file = getPreviewFile(fileId);
        try {
            return new FileDownloadResource(file.getOriginalName(), mimeType(file.getMimeType()), file.getSize(), fileStorageService.load(file.getStoragePath()));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    public SysFileEntity getPreviewFile(Long fileId) {
        SysFileEntity file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        return file;
    }

    private void updateChunk(String uploadId, Integer chunkIndex, MultipartFile chunk, String checksum, String storagePath) {
        SysFileUploadChunkEntity update = new SysFileUploadChunkEntity();
        update.setChunkSize(chunk.getSize());
        update.setChecksum(checksum);
        update.setStoragePath(storagePath);
        uploadChunkMapper.update(update, new LambdaUpdateWrapper<SysFileUploadChunkEntity>()
                .eq(SysFileUploadChunkEntity::getUploadId, uploadId)
                .eq(SysFileUploadChunkEntity::getChunkIndex, chunkIndex));
    }

    private SysFileEntity buildFileEntity(String originalName, String mimeType, String bizType, Long createdBy, FileStorageObject storageObject) {
        LocalDateTime now = LocalDateTime.now();
        SysFileEntity entity = new SysFileEntity();
        entity.setOriginalName(safeOriginalName(originalName));
        entity.setStorageKey(storageObject.storageKey());
        entity.setStoragePath(storageObject.storagePath());
        entity.setMimeType(mimeType(mimeType));
        entity.setExtension(storageObject.extension());
        entity.setSize(storageObject.size());
        entity.setChecksum(storageObject.checksum());
        entity.setBizType(bizType);
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setDeleted(0);
        return entity;
    }

    private SysFileUploadSessionEntity loadActiveSession(String uploadId, CurrentUserResponse currentUser) {
        SysFileUploadSessionEntity session = loadSession(uploadId, currentUser);
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            markSession(uploadId, FileUploadStatus.EXPIRED);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_SESSION_EXPIRED);
        }
        if (!FileUploadStatus.UPLOADING.name().equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_SESSION_EXPIRED);
        }
        return session;
    }

    private SysFileUploadSessionEntity loadSession(String uploadId, CurrentUserResponse currentUser) {
        SysFileUploadSessionEntity session = uploadSessionMapper.selectOne(new LambdaQueryWrapper<SysFileUploadSessionEntity>()
                .eq(SysFileUploadSessionEntity::getUploadId, uploadId)
                .eq(SysFileUploadSessionEntity::getCreatedBy, currentUser.id()));
        if (session == null) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_SESSION_NOT_FOUND);
        }
        return session;
    }

    private SysFileEntity loadOwnedFile(Long fileId, CurrentUserResponse currentUser) {
        SysFileEntity file = fileMapper.selectById(fileId);
        if (file == null || !currentUser.id().equals(file.getCreatedBy())) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        return file;
    }

    private void markSession(String uploadId, FileUploadStatus status) {
        SysFileUploadSessionEntity update = new SysFileUploadSessionEntity();
        update.setStatus(status.name());
        update.setUpdatedAt(LocalDateTime.now());
        uploadSessionMapper.update(update, new LambdaUpdateWrapper<SysFileUploadSessionEntity>()
                .eq(SysFileUploadSessionEntity::getUploadId, uploadId));
    }

    private void deleteChunkRecords(String uploadId) {
        uploadChunkMapper.delete(new LambdaQueryWrapper<SysFileUploadChunkEntity>()
                .eq(SysFileUploadChunkEntity::getUploadId, uploadId));
    }

    private List<Integer> uploadedChunks(String uploadId) {
        return uploadChunkMapper.selectList(new LambdaQueryWrapper<SysFileUploadChunkEntity>()
                        .eq(SysFileUploadChunkEntity::getUploadId, uploadId))
                .stream()
                .map(SysFileUploadChunkEntity::getChunkIndex)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private long expectedChunkSize(SysFileUploadSessionEntity session, int chunkIndex) {
        if (chunkIndex == session.getTotalChunks() - 1) {
            long remaining = session.getTotalSize() - (session.getChunkSize() * chunkIndex);
            return remaining == 0 ? session.getChunkSize() : remaining;
        }
        return session.getChunkSize();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }
        validateSize(file.getSize());
    }

    private void validateSize(long size) {
        if (size <= 0) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }
        if (size > fileProperties.getMaxSize().toBytes()) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
    }

    private String validateExtension(String fileName) {
        String extension = extension(fileName);
        if (extension.isBlank() || fileProperties.getAllowedExtensions().stream().noneMatch(value -> value.equalsIgnoreCase(extension))) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        return extension;
    }

    private String normalizeBizType(String bizType) {
        String normalized = bizType == null || bizType.isBlank() ? DEFAULT_BIZ_TYPE : bizType.trim();
        if (fileProperties.getAllowedBizTypes().isEmpty()) {
            return normalized;
        }
        boolean allowed = fileProperties.getAllowedBizTypes().stream().anyMatch(value -> value.equalsIgnoreCase(normalized));
        if (!allowed) {
            return DEFAULT_BIZ_TYPE;
        }
        return normalized;
    }

    private static String extension(String fileName) {
        String name = safeOriginalName(fileName);
        int index = name.lastIndexOf('.');
        return index < 0 ? "" : name.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private static String safeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "unnamed";
        }
        return originalName.replace('\\', '/').substring(originalName.replace('\\', '/').lastIndexOf('/') + 1);
    }

    private static String mimeType(String mimeType) {
        return mimeType == null || mimeType.isBlank() ? "application/octet-stream" : mimeType;
    }

    private FileVO toVO(SysFileEntity entity) {
        return new FileVO(
                entity.getId(),
                entity.getOriginalName(),
                "/api/files/" + entity.getId() + "/download",
                mimeType(entity.getMimeType()),
                entity.getSize(),
                entity.getChecksum(),
                entity.getBizType()
        );
    }

    private FileUploadInitResponse toInitResponse(SysFileUploadSessionEntity session, List<Integer> uploadedChunks) {
        return new FileUploadInitResponse(
                session.getUploadId(),
                session.getChunkSize(),
                session.getTotalChunks(),
                uploadedChunks,
                toOffset(session.getExpiresAt())
        );
    }

    private static OffsetDateTime toOffset(LocalDateTime value) {
        return value.atOffset(ZONE_OFFSET);
    }
}
