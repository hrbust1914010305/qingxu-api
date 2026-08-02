package com.qingxu.qingxuapi.application.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingxu.qingxuapi.common.exception.BusinessException;
import com.qingxu.qingxuapi.common.response.ErrorCode;
import com.qingxu.qingxuapi.common.response.PageResponse;
import com.qingxu.qingxuapi.application.notification.NotificationApplicationService;
import com.qingxu.qingxuapi.application.notification.NotificationCreateCommand;
import com.qingxu.qingxuapi.domain.file.FileStorageService;
import com.qingxu.qingxuapi.domain.knowledge.KnowledgeDocumentStatus;
import com.qingxu.qingxuapi.domain.knowledge.KnowledgeIndexJobStatus;
import com.qingxu.qingxuapi.domain.knowledge.KnowledgeIndexStage;
import com.qingxu.qingxuapi.domain.notification.NotificationLevel;
import com.qingxu.qingxuapi.domain.notification.NotificationType;
import com.qingxu.qingxuapi.infrastructure.ai.KnowledgeAiClient;
import com.qingxu.qingxuapi.infrastructure.ai.KnowledgeIndexAcceptedResponse;
import com.qingxu.qingxuapi.infrastructure.ai.KnowledgeIndexStartRequest;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KgEntityMentionEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KgRelationEvidenceEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KnowledgeBaseEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KnowledgeChunkEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KnowledgeIndexJobEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysFileEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KgAliasMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KgEntityMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KgEntityMentionMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KgExtractionJobMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KgRelationEvidenceMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KgRelationMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KnowledgeBaseMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KnowledgeChunkMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KnowledgeDocumentMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KnowledgeElementMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KnowledgeIndexJobMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysFileMapper;
import com.qingxu.qingxuapi.interfaces.auth.dto.CurrentUserResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.dto.KnowledgeChunkResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.dto.KnowledgeIndexedChunkRequest;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2AttachmentRequest;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2AttachmentResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2CreateRequest;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2CreateResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2ListItemResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2ParseRequest;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2ParseResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2ProgressCallbackRequest;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2ProgressDocumentResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2ProgressResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2RemoveResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2StageProgressResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2StopParseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeV2ApplicationService {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("txt", "md", "pdf", "docx", "xlsx", "csv", "pptx", "html");
    private static final Set<String> RUNNING_JOB_STATUSES = Set.of(KnowledgeIndexJobStatus.PENDING.name(), KnowledgeIndexJobStatus.RUNNING.name());
    private static final Set<String> TERMINAL_JOB_STATUSES = Set.of(KnowledgeIndexJobStatus.SUCCESS.name(), KnowledgeIndexJobStatus.FAILED.name(), KnowledgeIndexJobStatus.CANCELED.name());
    private static final List<String> DISPLAY_STAGES = List.of(
            KnowledgeIndexStage.LOAD_FILE.name(),
            KnowledgeIndexStage.PARSE_DOCUMENT.name(),
            KnowledgeIndexStage.NORMALIZE_TEXT.name(),
            KnowledgeIndexStage.DETECT_STRUCTURE.name(),
            KnowledgeIndexStage.BUILD_BLOCKS.name(),
            KnowledgeIndexStage.SPLIT_LONG_BLOCKS.name(),
            KnowledgeIndexStage.MERGE_SMALL_BLOCKS.name(),
            KnowledgeIndexStage.BUILD_OVERLAP.name(),
            KnowledgeIndexStage.FINALIZE_CHUNKS.name(),
            KnowledgeIndexStage.EMBED_CHUNKS.name(),
            KnowledgeIndexStage.STORE_VECTORS.name(),
            KnowledgeIndexStage.DONE.name()
    );

    private final KnowledgeBaseMapper baseMapper;
    private final SysFileMapper fileMapper;
    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeIndexJobMapper jobMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final KnowledgeElementMapper elementMapper;
    private final KgEntityMapper entityMapper;
    private final KgRelationMapper relationMapper;
    private final KgAliasMapper aliasMapper;
    private final KgEntityMentionMapper entityMentionMapper;
    private final KgRelationEvidenceMapper relationEvidenceMapper;
    private final KgExtractionJobMapper extractionJobMapper;
    private final KnowledgeAiClient aiClient;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final NotificationApplicationService notificationApplicationService;

    @Transactional
    public KnowledgeV2CreateResponse create(KnowledgeV2CreateRequest request, CurrentUserResponse currentUser) {
        LocalDateTime now = LocalDateTime.now();
        KnowledgeBaseEntity base = new KnowledgeBaseEntity();
        base.setName(request.name().trim());
        base.setDescription(blankToNull(request.description()));
        base.setVisibility(defaultVisibility(request.visibility()));
        base.setGraphDomain(defaultGraphDomain(request.graphDomain()));
        base.setCreatedBy(currentUser.id());
        base.setCreatedAt(now);
        base.setUpdatedAt(now);
        base.setDeleted(0);
        baseMapper.insert(base);

        List<KnowledgeV2AttachmentResponse> attachments = new ArrayList<>();
        for (KnowledgeV2AttachmentRequest attachment : safeAttachments(request.attachments())) {
            SysFileEntity file = loadOwnedKnowledgeFile(attachment.fileId(), currentUser);
            KnowledgeDocumentEntity existing = findDocument(base.getId(), file.getId());
            if (existing == null) {
                existing = createUploadedDocument(base.getId(), file, currentUser.id(), now);
            }
            attachments.add(toAttachmentResponse(existing, file));
        }
        return new KnowledgeV2CreateResponse(base.getId(), base.getName(), base.getDescription(), base.getVisibility(), base.getGraphDomain(), attachments.size(), attachments);
    }

    public PageResponse<KnowledgeV2ListItemResponse> list(String keyword, String visibility, String status, long page, long pageSize, CurrentUserResponse currentUser) {
        LambdaQueryWrapper<KnowledgeBaseEntity> wrapper = new LambdaQueryWrapper<KnowledgeBaseEntity>()
                .eq(KnowledgeBaseEntity::getCreatedBy, currentUser.id())
                .eq(KnowledgeBaseEntity::getDeleted, 0)
                .orderByDesc(KnowledgeBaseEntity::getCreatedAt);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(KnowledgeBaseEntity::getName, keyword.trim());
        }
        if (visibility != null && !visibility.isBlank()) {
            wrapper.eq(KnowledgeBaseEntity::getVisibility, visibility.trim().toUpperCase(Locale.ROOT));
        }
        if (status != null && !status.isBlank()) {
            String expectedStatus = status.trim().toUpperCase(Locale.ROOT);
            List<KnowledgeV2ListItemResponse> filtered = baseMapper.selectList(wrapper).stream()
                    .map(this::toListItem)
                    .filter(item -> expectedStatus.equals(item.status()))
                    .toList();
            long fromIndex = Math.max(0, (page - 1) * pageSize);
            long toIndex = Math.min(filtered.size(), fromIndex + pageSize);
            List<KnowledgeV2ListItemResponse> records = fromIndex >= filtered.size()
                    ? List.of()
                    : filtered.subList(Math.toIntExact(fromIndex), Math.toIntExact(toIndex));
            return new PageResponse<>(records, filtered.size(), page, pageSize);
        }
        Page<KnowledgeBaseEntity> result = baseMapper.selectPage(Page.of(page, pageSize), wrapper);
        return new PageResponse<>(
                result.getRecords().stream().map(this::toListItem).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    @Transactional
    public KnowledgeV2ParseResponse parse(KnowledgeV2ParseRequest request, CurrentUserResponse currentUser) {
        KnowledgeBaseEntity base = loadOwnedKnowledgeBase(request.knowledgeId(), currentUser);
        List<KnowledgeDocumentEntity> documents = documentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, base.getId())
                .eq(KnowledgeDocumentEntity::getDeleted, 0)
                .in(KnowledgeDocumentEntity::getStatus, KnowledgeDocumentStatus.UPLOADED.name(), KnowledgeDocumentStatus.FAILED.name()));
        if (documents.isEmpty()) {
            return new KnowledgeV2ParseResponse(base.getId(), 0, 0, 0, "没有待解析文档");
        }
        aiClient.ensureAvailable();
        int started = 0;
        for (KnowledgeDocumentEntity document : documents) {
            if (hasRunningJob(document.getId())) {
                continue;
            }
            startDocumentIndexing(document, currentUser);
            started++;
        }
        return new KnowledgeV2ParseResponse(base.getId(), documents.size(), started, documents.size() - started, "解析任务已创建");
    }

    @Transactional
    public KnowledgeV2ParseResponse reparseDocument(Long documentId, CurrentUserResponse currentUser) {
        KnowledgeDocumentEntity document = loadOwnedDocument(documentId, currentUser);
        loadOwnedKnowledgeBase(document.getKnowledgeBaseId(), currentUser);
        if (hasRunningJob(document.getId())) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_INDEX_JOB_RUNNING);
        }
        aiClient.ensureAvailable();
        cleanupDocumentIndexData(document.getId());
        startDocumentIndexing(document, currentUser);
        return new KnowledgeV2ParseResponse(document.getKnowledgeBaseId(), 1, 1, 0, "解析任务已创建");
    }

    @Transactional
    public KnowledgeV2StopParseResponse stopParse(KnowledgeV2ParseRequest request, CurrentUserResponse currentUser) {

        KnowledgeBaseEntity base = loadOwnedKnowledgeBase(request.knowledgeId(), currentUser);
        List<KnowledgeIndexJobEntity> jobs = jobMapper.selectList(new LambdaQueryWrapper<KnowledgeIndexJobEntity>()
                .eq(KnowledgeIndexJobEntity::getKnowledgeBaseId, base.getId())
                .in(KnowledgeIndexJobEntity::getStatus, RUNNING_JOB_STATUSES));
        LocalDateTime now = LocalDateTime.now();
        int canceled = 0;
        for (KnowledgeIndexJobEntity job : jobs) {
            requestAiCancel(job);
            job.setStatus(KnowledgeIndexJobStatus.CANCELED.name());
            job.setErrorMessage("解析已停止");
            job.setFinishedAt(now);
            job.setUpdatedAt(now);
            jobMapper.updateById(job);

            KnowledgeDocumentEntity document = documentMapper.selectById(job.getDocumentId());
            if (document != null && currentUser.id().equals(document.getCreatedBy())) {
                document.setStatus(KnowledgeDocumentStatus.UPLOADED.name());
                document.setErrorMessage("解析已停止");
                document.setUpdatedAt(now);
                documentMapper.updateById(document);
                canceled++;
            }
        }
        if (canceled > 0) {
            createKnowledgeIndexNotification(base, NotificationLevel.WARNING, "知识库解析已停止", "知识库「" + base.getName() + "」解析已停止");
        }
        return new KnowledgeV2StopParseResponse(base.getId(), canceled, "解析已停止");
    }

    public KnowledgeV2ProgressResponse progress(Long knowledgeId, CurrentUserResponse currentUser) {
        KnowledgeBaseEntity base = loadOwnedKnowledgeBase(knowledgeId, currentUser);
        List<KnowledgeDocumentEntity> documents = documentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, base.getId())
                .eq(KnowledgeDocumentEntity::getDeleted, 0));
        List<KnowledgeIndexJobEntity> latestJobs = documents.stream()
                .map(document -> latestJob(document.getId()))
                .toList();
        List<KnowledgeV2ProgressDocumentResponse> items = documents.stream()
                .map(document -> toProgressDocument(document, latestJob(document.getId())))
                .toList();
        int total = items.size();
        int finished = (int) items.stream().filter(item -> KnowledgeDocumentStatus.INDEXED.name().equals(item.status())).count();
        int failed = (int) items.stream().filter(item -> KnowledgeDocumentStatus.FAILED.name().equals(item.status())).count();
        int canceled = (int) items.stream().filter(item -> KnowledgeIndexJobStatus.CANCELED.name().equals(item.status()) || "解析已停止".equals(item.errorMessage())).count();
        int progress = total == 0 ? 0 : (int) Math.round(items.stream().mapToInt(item -> item.progress() == null ? 0 : item.progress()).average().orElse(0));
        boolean running = latestJobs.stream().anyMatch(this::isRunningJob);
        String status = running ? KnowledgeDocumentStatus.PARSING.name() : resolveKnowledgeStatus(total, finished, failed, canceled, items);
        Integer estimatedRemainingSeconds = maxEstimatedRemainingSeconds(latestJobs);
        List<KnowledgeV2StageProgressResponse> stages = buildStageProgress(latestJobs);
        String message = resolveProgressMessage(status, stages);
        return new KnowledgeV2ProgressResponse(base.getId(), status, running, estimatedRemainingSeconds, message, total, finished, failed, canceled, progress, stages, items);
    }

    public PageResponse<KnowledgeChunkResponse> chunks(Long knowledgeId, Long documentId, long page, long pageSize, String keyword, String contentType, CurrentUserResponse currentUser) {
        KnowledgeBaseEntity base = loadOwnedKnowledgeBase(knowledgeId, currentUser);
        LambdaQueryWrapper<KnowledgeChunkEntity> wrapper = new LambdaQueryWrapper<KnowledgeChunkEntity>()
                .eq(KnowledgeChunkEntity::getKnowledgeBaseId, base.getId())
                .eq(KnowledgeChunkEntity::getDeleted, 0)
                .orderByAsc(KnowledgeChunkEntity::getDocumentId)
                .orderByAsc(KnowledgeChunkEntity::getChunkIndex);
        if (documentId != null) {
            KnowledgeDocumentEntity document = documentMapper.selectById(documentId);
            if (document == null || !base.getId().equals(document.getKnowledgeBaseId()) || !currentUser.id().equals(document.getCreatedBy())) {
                throw new BusinessException(ErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND);
            }
            wrapper.eq(KnowledgeChunkEntity::getDocumentId, documentId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(KnowledgeChunkEntity::getContent, keyword.trim());
        }
        if (contentType != null && !contentType.isBlank()) {
            wrapper.eq(KnowledgeChunkEntity::getContentType, contentType.trim());
        }
        Page<KnowledgeChunkEntity> result = chunkMapper.selectPage(Page.of(page, pageSize), wrapper);
        return new PageResponse<>(
                result.getRecords().stream().map(this::toChunkResponse).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    @Transactional
    public KnowledgeV2RemoveResponse remove(Long knowledgeId, CurrentUserResponse currentUser) {
        KnowledgeBaseEntity base = loadOwnedKnowledgeBase(knowledgeId, currentUser);
        List<KnowledgeIndexJobEntity> runningJobs = jobMapper.selectList(new LambdaQueryWrapper<KnowledgeIndexJobEntity>()
                .eq(KnowledgeIndexJobEntity::getKnowledgeBaseId, base.getId())
                .in(KnowledgeIndexJobEntity::getStatus, RUNNING_JOB_STATUSES));
        cancelRunningJobs(base.getId(), runningJobs, currentUser);
        List<KnowledgeDocumentEntity> documents = documentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, base.getId())
                .eq(KnowledgeDocumentEntity::getDeleted, 0));
        List<Long> documentIds = documents.stream().map(KnowledgeDocumentEntity::getId).toList();
        Set<Long> removedFiles = new HashSet<>();
        if (!documentIds.isEmpty()) {
            cleanupDocumentData(documentIds);
        }
        for (KnowledgeDocumentEntity document : documents) {
            documentMapper.deleteById(document.getId());
            if (canRemoveFile(document.getFileId(), base.getId())) {
                removeFile(document.getFileId());
                removedFiles.add(document.getFileId());
            }
        }
        cleanupGraphData(base.getId());
        baseMapper.deleteById(base.getId());
        return new KnowledgeV2RemoveResponse(base.getId(), documents.size(), removedFiles.size(), "知识库已删除");
    }

    @Transactional
    public void reportProgress(KnowledgeV2ProgressCallbackRequest request) {
        KnowledgeIndexJobEntity job = jobMapper.selectById(request.jobId());
        if (job == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_INDEX_JOB_NOT_FOUND);
        }
        if (KnowledgeIndexJobStatus.CANCELED.name().equals(job.getStatus())) {
            log.info("[knowledge-v2] ignore progress callback for canceled job: jobId={}", job.getId());
            return;
        }
        KnowledgeDocumentEntity document = documentMapper.selectById(request.documentId());
        if (document == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        boolean wasTerminal = TERMINAL_JOB_STATUSES.contains(job.getStatus());
        job.setStatus(toJobStatus(request.status()));
        job.setStage(request.stage() == null ? job.getStage() : request.stage());
        job.setProgress(request.progress() == null ? job.getProgress() : request.progress());
        job.setEstimatedRemainingSeconds(request.estimatedRemainingSeconds());
        job.setErrorMessage(request.errorMessage());
        job.setUpdatedAt(now);
        if (KnowledgeIndexJobStatus.RUNNING.name().equals(job.getStatus()) && job.getStartedAt() == null) {
            job.setStartedAt(now);
        }
        if (KnowledgeIndexJobStatus.SUCCESS.name().equals(job.getStatus())
                || KnowledgeIndexJobStatus.FAILED.name().equals(job.getStatus())
                || KnowledgeIndexJobStatus.CANCELED.name().equals(job.getStatus())) {
            job.setFinishedAt(now);
        }
        jobMapper.updateById(job);

        storeChunksIfPresent(request, job, document, now);

        document.setStatus(request.status());
        document.setChunkCount(request.chunkCount() == null ? document.getChunkCount() : request.chunkCount());
        document.setGraphReady(request.graphReady() == null ? document.getGraphReady() : request.graphReady());
        document.setErrorMessage(request.errorMessage());
        document.setUpdatedAt(now);
        documentMapper.updateById(document);
        if (!wasTerminal && TERMINAL_JOB_STATUSES.contains(job.getStatus()) && !hasRunningJobInKnowledgeBase(job.getKnowledgeBaseId())) {
            notifyKnowledgeIndexFinished(job.getKnowledgeBaseId());
        }
    }

    private KnowledgeBaseEntity loadOwnedKnowledgeBase(Long knowledgeId, CurrentUserResponse currentUser) {
        KnowledgeBaseEntity base = baseMapper.selectById(knowledgeId);
        if (base == null || Integer.valueOf(1).equals(base.getDeleted()) || !currentUser.id().equals(base.getCreatedBy())) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }
        return base;
    }

    private SysFileEntity loadOwnedKnowledgeFile(Long fileId, CurrentUserResponse currentUser) {
        SysFileEntity file = fileMapper.selectById(fileId);
        if (file == null || Integer.valueOf(1).equals(file.getDeleted()) || !currentUser.id().equals(file.getCreatedBy())) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        if (!"knowledge".equalsIgnoreCase(file.getBizType())) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_BIZ_TYPE_INVALID);
        }
        String extension = file.getExtension() == null ? "" : file.getExtension().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_TYPE_NOT_SUPPORTED);
        }
        return file;
    }

    private KnowledgeDocumentEntity loadOwnedDocument(Long documentId, CurrentUserResponse currentUser) {
        KnowledgeDocumentEntity document = documentMapper.selectById(documentId);
        if (document == null || Integer.valueOf(1).equals(document.getDeleted()) || !currentUser.id().equals(document.getCreatedBy())) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND);
        }
        return document;
    }

    private KnowledgeDocumentEntity findDocument(Long knowledgeId, Long fileId) {
        return documentMapper.selectOne(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, knowledgeId)
                .eq(KnowledgeDocumentEntity::getFileId, fileId)
                .eq(KnowledgeDocumentEntity::getDeleted, 0)
                .last("limit 1"));
    }

    private KnowledgeDocumentEntity createUploadedDocument(Long knowledgeId, SysFileEntity file, Long userId, LocalDateTime now) {
        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
        document.setKnowledgeBaseId(knowledgeId);
        document.setFileId(file.getId());
        document.setOriginalName(file.getOriginalName());
        document.setMimeType(file.getMimeType());
        document.setExtension(file.getExtension());
        document.setChecksum(file.getChecksum());
        document.setStatus(KnowledgeDocumentStatus.UPLOADED.name());
        document.setChunkCount(0);
        document.setGraphReady(false);
        document.setCreatedBy(userId);
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        document.setDeleted(0);
        documentMapper.insert(document);
        return document;
    }

    private void markDocumentPending(KnowledgeDocumentEntity document) {
        document.setStatus(KnowledgeDocumentStatus.PENDING.name());
        document.setErrorMessage(null);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
    }

    private void startDocumentIndexing(KnowledgeDocumentEntity document, CurrentUserResponse currentUser) {
        SysFileEntity file = loadOwnedKnowledgeFile(document.getFileId(), currentUser);
        markDocumentPending(document);
        KnowledgeIndexJobEntity job = createJob(document, file, currentUser.id());
        KnowledgeIndexAcceptedResponse accepted = aiClient.startIndexingAccepted(toAiRequest(job, document, file, currentUser.id()));
        ensureAiAccepted(job.getId(), accepted);
    }

    private KnowledgeIndexJobEntity createJob(KnowledgeDocumentEntity document, SysFileEntity file, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        KnowledgeIndexJobEntity job = new KnowledgeIndexJobEntity();
        job.setDocumentId(document.getId());
        job.setFileId(file.getId());
        job.setKnowledgeBaseId(document.getKnowledgeBaseId());
        job.setStatus(KnowledgeIndexJobStatus.PENDING.name());
        job.setStage(KnowledgeIndexStage.LOAD_FILE.name());
        job.setProgress(0);
        job.setEstimatedSeconds(null);
        job.setEstimatedRemainingSeconds(null);
        job.setRetryCount(0);
        job.setMaxRetryCount(3);
        job.setCreatedBy(userId);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        jobMapper.insert(job);
        return job;
    }

    private boolean hasRunningJob(Long documentId) {
        List<KnowledgeIndexJobEntity> jobs = jobMapper.selectList(new LambdaQueryWrapper<KnowledgeIndexJobEntity>()
                .eq(KnowledgeIndexJobEntity::getDocumentId, documentId)
                .in(KnowledgeIndexJobEntity::getStatus, RUNNING_JOB_STATUSES));
        return !jobs.isEmpty();
    }

    private KnowledgeIndexJobEntity latestJob(Long documentId) {
        return jobMapper.selectOne(new LambdaQueryWrapper<KnowledgeIndexJobEntity>()
                .eq(KnowledgeIndexJobEntity::getDocumentId, documentId)
                .orderByDesc(KnowledgeIndexJobEntity::getId)
                .last("limit 1"));
    }

    private KnowledgeIndexStartRequest toAiRequest(KnowledgeIndexJobEntity job, KnowledgeDocumentEntity document, SysFileEntity file, Long userId) {
        return new KnowledgeIndexStartRequest(
                job.getId(),
                document.getId(),
                file.getId(),
                document.getKnowledgeBaseId(),
                file.getStoragePath(),
                file.getOriginalName(),
                file.getMimeType(),
                file.getExtension(),
                file.getChecksum(),
                userId
        );
    }

    private KnowledgeV2AttachmentResponse toAttachmentResponse(KnowledgeDocumentEntity document, SysFileEntity file) {
        return new KnowledgeV2AttachmentResponse(
                document.getId(),
                file.getId(),
                file.getOriginalName(),
                file.getStoragePath(),
                file.getMimeType(),
                file.getExtension(),
                file.getSize(),
                file.getChecksum(),
                file.getBizType(),
                document.getStatus(),
                file.getCreatedAt(),
                file.getUpdatedAt()
        );
    }

    private void cancelRunningJobs(Long knowledgeId, List<KnowledgeIndexJobEntity> jobs, CurrentUserResponse currentUser) {
        if (jobs == null || jobs.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (KnowledgeIndexJobEntity job : jobs) {
            requestAiCancel(job);
            job.setStatus(KnowledgeIndexJobStatus.CANCELED.name());
            job.setErrorMessage("解析已停止");
            job.setFinishedAt(now);
            job.setUpdatedAt(now);
            jobMapper.updateById(job);

            KnowledgeDocumentEntity document = documentMapper.selectById(job.getDocumentId());
            if (document == null || !currentUser.id().equals(document.getCreatedBy())) {
                continue;
            }
            document.setStatus(KnowledgeDocumentStatus.UPLOADED.name());
            document.setErrorMessage("解析已停止");
            document.setUpdatedAt(now);
            documentMapper.updateById(document);
        }
    }

    private void requestAiCancel(KnowledgeIndexJobEntity job) {
        if (job == null || job.getId() == null) {
            return;
        }
        try {
            aiClient.cancelIndexing(job.getId());
        } catch (RuntimeException exception) {
            log.warn("[knowledge-v2] cancel AI index job failed, continue local cancel: jobId={}, documentId={}, message={}",
                    job.getId(), job.getDocumentId(), exception.getMessage(), exception);
        }
    }

    private boolean hasRunningJobInKnowledgeBase(Long knowledgeId) {
        Long count = jobMapper.selectCount(new LambdaQueryWrapper<KnowledgeIndexJobEntity>()
                .eq(KnowledgeIndexJobEntity::getKnowledgeBaseId, knowledgeId)
                .in(KnowledgeIndexJobEntity::getStatus, RUNNING_JOB_STATUSES));
        return count != null && count > 0;
    }

    private void notifyKnowledgeIndexFinished(Long knowledgeId) {
        KnowledgeBaseEntity base = baseMapper.selectById(knowledgeId);
        if (base == null || Integer.valueOf(1).equals(base.getDeleted())) {
            return;
        }
        Long failedCount = documentMapper.selectCount(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, knowledgeId)
                .eq(KnowledgeDocumentEntity::getDeleted, 0)
                .eq(KnowledgeDocumentEntity::getStatus, KnowledgeDocumentStatus.FAILED.name()));
        if (failedCount != null && failedCount > 0) {
            createKnowledgeIndexNotification(base, NotificationLevel.ERROR, "知识库解析失败", "知识库「" + base.getName() + "」解析失败，请查看状态");
        } else {
            createKnowledgeIndexNotification(base, NotificationLevel.SUCCESS, "知识库解析完成", "知识库「" + base.getName() + "」已解析完成");
        }
    }

    private void createKnowledgeIndexNotification(KnowledgeBaseEntity base, NotificationLevel level, String title, String content) {
        notificationApplicationService.createNotification(new NotificationCreateCommand(
                NotificationType.TASK,
                level,
                title,
                content,
                "/knowledge",
                "KNOWLEDGE_INDEX",
                String.valueOf(base.getId()),
                null,
                null,
                Set.of(base.getCreatedBy()),
                UUID.randomUUID().toString()
        ));
    }

    private void ensureAiAccepted(Long jobId, KnowledgeIndexAcceptedResponse accepted) {
        if (accepted == null || !Boolean.TRUE.equals(accepted.accepted())) {
            throw new IllegalStateException("Knowledge AI index job was not accepted: jobId=" + jobId);
        }
    }

    private KnowledgeV2ListItemResponse toListItem(KnowledgeBaseEntity base) {
        Integer documentCount = countDocuments(base.getId(), null);
        Integer indexedCount = countDocuments(base.getId(), KnowledgeDocumentStatus.INDEXED.name());
        Integer failedCount = countDocuments(base.getId(), KnowledgeDocumentStatus.FAILED.name());
        String status = resolveKnowledgeStatusForBase(base.getId());
        Integer chunkCount = Math.toIntExact(chunkMapper.selectCount(new LambdaQueryWrapper<KnowledgeChunkEntity>()
                .eq(KnowledgeChunkEntity::getKnowledgeBaseId, base.getId())
                .eq(KnowledgeChunkEntity::getDeleted, 0)));
        List<KnowledgeV2AttachmentResponse> attachments = listAttachments(base.getId());
        return new KnowledgeV2ListItemResponse(
                base.getId(),
                base.getName(),
                base.getDescription(),
                base.getVisibility(),
                base.getGraphDomain(),
                status,
                documentCount,
                indexedCount,
                failedCount,
                chunkCount,
                attachments,
                base.getCreatedAt(),
                base.getUpdatedAt()
        );
    }

    /**
     * 规范化知识库图谱领域，旧请求未传时默认 AUTO。
     *
     * @param graphDomain 前端传入的图谱领域
     * @return 用于保存和传递给 AI 服务的领域编码
     */
    private String defaultGraphDomain(String graphDomain) {
        if (graphDomain == null || graphDomain.isBlank()) {
            return "AUTO";
        }
        return graphDomain.trim().toUpperCase(Locale.ROOT);
    }

    private List<KnowledgeV2AttachmentResponse> listAttachments(Long knowledgeId) {
        return documentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                        .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, knowledgeId)
                        .eq(KnowledgeDocumentEntity::getDeleted, 0)
                        .orderByAsc(KnowledgeDocumentEntity::getId))
                .stream()
                .map(document -> {
                    SysFileEntity file = fileMapper.selectById(document.getFileId());
                    return file == null ? null : toAttachmentResponse(document, file);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private String resolveKnowledgeStatusForBase(Long knowledgeId) {
        List<KnowledgeDocumentEntity> documents = documentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, knowledgeId)
                .eq(KnowledgeDocumentEntity::getDeleted, 0));
        if (documents.isEmpty()) {
            return KnowledgeDocumentStatus.UPLOADED.name();
        }
        List<KnowledgeV2ProgressDocumentResponse> items = documents.stream()
                .map(document -> toProgressDocument(document, latestJob(document.getId())))
                .toList();
        int total = items.size();
        int finished = (int) items.stream().filter(item -> KnowledgeDocumentStatus.INDEXED.name().equals(item.status())).count();
        int failed = (int) items.stream().filter(item -> KnowledgeDocumentStatus.FAILED.name().equals(item.status())).count();
        int canceled = (int) items.stream().filter(item -> "解析已停止".equals(item.errorMessage())).count();
        return resolveKnowledgeStatus(total, finished, failed, canceled, items);
    }

    private Integer countDocuments(Long knowledgeId, String status) {
        LambdaQueryWrapper<KnowledgeDocumentEntity> wrapper = new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, knowledgeId)
                .eq(KnowledgeDocumentEntity::getDeleted, 0);
        if (status != null) {
            wrapper.eq(KnowledgeDocumentEntity::getStatus, status);
        }
        return Math.toIntExact(documentMapper.selectCount(wrapper));
    }

    private KnowledgeV2ProgressDocumentResponse toProgressDocument(KnowledgeDocumentEntity document, KnowledgeIndexJobEntity job) {
        boolean running = isRunningJob(job);
        return new KnowledgeV2ProgressDocumentResponse(
                document.getId(),
                document.getFileId(),
                document.getOriginalName(),
                job == null ? null : job.getId(),
                job == null ? null : job.getId(),
                resolveDocumentStatus(document, job),
                job == null ? null : job.getStage(),
                job == null ? 0 : job.getProgress(),
                running,
                estimateRemainingSeconds(job),
                document.getChunkCount(),
                document.getGraphReady(),
                document.getErrorMessage(),
                job == null ? null : job.getStartedAt(),
                job == null ? null : job.getFinishedAt(),
                resolveDocumentProgressMessage(document, job)
        );
    }

    private String resolveDocumentStatus(KnowledgeDocumentEntity document, KnowledgeIndexJobEntity job) {
        if (job == null) {
            return document.getStatus();
        }
        if (KnowledgeIndexJobStatus.CANCELED.name().equals(job.getStatus())) {
            return KnowledgeIndexJobStatus.CANCELED.name();
        }
        if (KnowledgeIndexJobStatus.SUCCESS.name().equals(job.getStatus())) {
            return KnowledgeDocumentStatus.INDEXED.name();
        }
        if (KnowledgeIndexJobStatus.FAILED.name().equals(job.getStatus())) {
            return KnowledgeDocumentStatus.FAILED.name();
        }
        return document.getStatus();
    }

    private List<KnowledgeV2StageProgressResponse> buildStageProgress(List<KnowledgeIndexJobEntity> jobs) {
        List<KnowledgeIndexJobEntity> effectiveJobs = jobs.stream().filter(job -> job != null).toList();
        if (effectiveJobs.isEmpty()) {
            return DISPLAY_STAGES.stream()
                    .map(stage -> new KnowledgeV2StageProgressResponse(stage, stageName(stage), "PENDING", 0, false, null))
                    .toList();
        }
        return DISPLAY_STAGES.stream()
                .map(stage -> toStageProgress(stage, effectiveJobs))
                .toList();
    }

    private KnowledgeV2StageProgressResponse toStageProgress(String stage, List<KnowledgeIndexJobEntity> jobs) {
        int stageIndex = stageIndex(stage);
        boolean running = jobs.stream().anyMatch(job -> isRunningJob(job) && stage.equals(normalizeStage(job.getStage())));
        boolean failed = jobs.stream().anyMatch(job -> KnowledgeIndexJobStatus.FAILED.name().equals(job.getStatus()) && stage.equals(normalizeStage(job.getStage())));
        boolean canceled = jobs.stream().anyMatch(job -> KnowledgeIndexJobStatus.CANCELED.name().equals(job.getStatus()) && stage.equals(normalizeStage(job.getStage())));
        boolean allPassed = jobs.stream().allMatch(job -> hasPassedStage(job, stageIndex));
        String status;
        if (running) {
            status = "RUNNING";
        } else if (failed) {
            status = "FAILED";
        } else if (canceled) {
            status = "CANCELED";
        } else if (allPassed) {
            status = "DONE";
        } else {
            status = "PENDING";
        }
        Integer progress = "DONE".equals(status) ? 100 : "RUNNING".equals(status) ? runningStageProgress(stage, jobs) : 0;
        Integer remaining = "RUNNING".equals(status) ? maxEstimatedRemainingSeconds(jobs.stream()
                .filter(job -> stage.equals(normalizeStage(job.getStage())))
                .toList()) : null;
        return new KnowledgeV2StageProgressResponse(stage, stageName(stage), status, progress, "RUNNING".equals(status), remaining);
    }

    private boolean hasPassedStage(KnowledgeIndexJobEntity job, int stageIndex) {
        if (job == null) {
            return false;
        }
        if (KnowledgeIndexJobStatus.SUCCESS.name().equals(job.getStatus())) {
            return true;
        }
        return stageIndex(normalizeStage(job.getStage())) > stageIndex;
    }

    private Integer runningStageProgress(String stage, List<KnowledgeIndexJobEntity> jobs) {
        return (int) Math.round(jobs.stream()
                .filter(job -> isRunningJob(job) && stage.equals(normalizeStage(job.getStage())))
                .mapToInt(job -> job.getProgress() == null ? 0 : job.getProgress())
                .average()
                .orElse(0));
    }

    private Integer maxEstimatedRemainingSeconds(List<KnowledgeIndexJobEntity> jobs) {
        return jobs.stream()
                .filter(this::isRunningJob)
                .map(this::estimateRemainingSeconds)
                .filter(value -> value != null && value >= 0)
                .max(Integer::compareTo)
                .orElse(null);
    }

    private Integer estimateRemainingSeconds(KnowledgeIndexJobEntity job) {
        if (job == null || !isRunningJob(job)) {
            return null;
        }
        return job.getEstimatedRemainingSeconds();
    }

    private boolean isRunningJob(KnowledgeIndexJobEntity job) {
        return job != null && RUNNING_JOB_STATUSES.contains(job.getStatus());
    }

    private int stageIndex(String stage) {
        int index = DISPLAY_STAGES.indexOf(normalizeStage(stage));
        return index < 0 ? 0 : index;
    }

    private String normalizeStage(String stage) {
        if (stage == null || stage.isBlank()) {
            return KnowledgeIndexStage.LOAD_FILE.name();
        }
        if (KnowledgeIndexStage.NORMALIZE_ELEMENTS.name().equals(stage)) {
            return KnowledgeIndexStage.NORMALIZE_TEXT.name();
        }
        if (KnowledgeIndexStage.CHUNK_ELEMENTS.name().equals(stage)) {
            return KnowledgeIndexStage.FINALIZE_CHUNKS.name();
        }
        if (KnowledgeIndexStage.EXTRACT_GRAPH.name().equals(stage) || KnowledgeIndexStage.STORE_GRAPH.name().equals(stage)) {
            return KnowledgeIndexStage.STORE_VECTORS.name();
        }
        return stage;
    }

    private String stageName(String stage) {
        return switch (normalizeStage(stage)) {
            case "LOAD_FILE" -> "读取文件";
            case "PARSE_DOCUMENT" -> "解析文档";
            case "NORMALIZE_TEXT" -> "清洗文本";
            case "DETECT_STRUCTURE" -> "识别结构";
            case "BUILD_BLOCKS" -> "生成文本块";
            case "SPLIT_LONG_BLOCKS" -> "拆分长文本";
            case "MERGE_SMALL_BLOCKS" -> "合并短文本";
            case "BUILD_OVERLAP" -> "补充上下文";
            case "FINALIZE_CHUNKS" -> "生成切片";
            case "EMBED_CHUNKS" -> "生成向量";
            case "STORE_VECTORS" -> "写入索引";
            case "DONE" -> "解析完成";
            default -> stage;
        };
    }

    private String resolveProgressMessage(String status, List<KnowledgeV2StageProgressResponse> stages) {
        return stages.stream()
                .filter(stage -> Boolean.TRUE.equals(stage.current()))
                .findFirst()
                .map(stage -> "正在" + stage.name())
                .orElseGet(() -> switch (status) {
                    case "INDEXED" -> "解析完成";
                    case "FAILED" -> "解析失败";
                    case "CANCELED" -> "解析已停止";
                    default -> "等待解析";
                });
    }

    private String resolveDocumentProgressMessage(KnowledgeDocumentEntity document, KnowledgeIndexJobEntity job) {
        if (job == null) {
            return "等待解析";
        }
        if (KnowledgeIndexJobStatus.FAILED.name().equals(job.getStatus())) {
            return document.getErrorMessage() == null ? "解析失败" : document.getErrorMessage();
        }
        if (KnowledgeIndexJobStatus.CANCELED.name().equals(job.getStatus())) {
            return "解析已停止";
        }
        if (KnowledgeIndexJobStatus.SUCCESS.name().equals(job.getStatus())) {
            return "解析完成";
        }
        return "正在" + stageName(job.getStage());
    }

    private String resolveKnowledgeStatus(int total, int finished, int failed, int canceled, List<KnowledgeV2ProgressDocumentResponse> items) {
        if (total == 0) {
            return KnowledgeDocumentStatus.UPLOADED.name();
        }
        boolean processing = items.stream().anyMatch(item -> Set.of(
                KnowledgeDocumentStatus.PENDING.name(),
                KnowledgeDocumentStatus.PARSING.name(),
                KnowledgeDocumentStatus.NORMALIZING.name(),
                KnowledgeDocumentStatus.STRUCTURING.name(),
                KnowledgeDocumentStatus.CHUNKING.name(),
                KnowledgeDocumentStatus.EMBEDDING.name(),
                KnowledgeDocumentStatus.STORING.name(),
                KnowledgeDocumentStatus.GRAPHING.name()
        ).contains(item.status()));
        if (processing) {
            return KnowledgeDocumentStatus.PARSING.name();
        }
        if (failed > 0) {
            return KnowledgeDocumentStatus.FAILED.name();
        }
        if (canceled > 0) {
            return KnowledgeIndexJobStatus.CANCELED.name();
        }
        return finished == total ? KnowledgeDocumentStatus.INDEXED.name() : KnowledgeDocumentStatus.UPLOADED.name();
    }

    private void cleanupDocumentIndexData(Long documentId) {
        KnowledgeChunkEntity chunkDelete = new KnowledgeChunkEntity();
        chunkDelete.setDeleted(1);
        chunkDelete.setUpdatedAt(LocalDateTime.now());
        chunkMapper.update(chunkDelete, new LambdaQueryWrapper<KnowledgeChunkEntity>()
                .eq(KnowledgeChunkEntity::getDocumentId, documentId)
                .eq(KnowledgeChunkEntity::getDeleted, 0));
        elementMapper.delete(new LambdaQueryWrapper<com.qingxu.qingxuapi.infrastructure.persistence.entity.KnowledgeElementEntity>()
                .eq(com.qingxu.qingxuapi.infrastructure.persistence.entity.KnowledgeElementEntity::getDocumentId, documentId));
        entityMentionMapper.delete(new LambdaQueryWrapper<KgEntityMentionEntity>()
                .eq(KgEntityMentionEntity::getDocumentId, documentId));
        relationEvidenceMapper.delete(new LambdaQueryWrapper<KgRelationEvidenceEntity>()
                .eq(KgRelationEvidenceEntity::getDocumentId, documentId));
        extractionJobMapper.delete(new LambdaQueryWrapper<com.qingxu.qingxuapi.infrastructure.persistence.entity.KgExtractionJobEntity>()
                .eq(com.qingxu.qingxuapi.infrastructure.persistence.entity.KgExtractionJobEntity::getDocumentId, documentId));
    }

    private KnowledgeChunkResponse toChunkResponse(KnowledgeChunkEntity chunk) {
        return new KnowledgeChunkResponse(
                chunk.getId(),
                chunk.getChunkIndex(),
                chunk.getContent(),
                chunk.getContentType(),
                chunk.getTitlePath(),
                chunk.getPageNumber(),
                chunk.getSheetName(),
                chunk.getTokenCount(),
                chunk.getEmbeddingStatus()
        );
    }

    private void cleanupDocumentData(List<Long> documentIds) {
        KnowledgeChunkEntity chunkDelete = new KnowledgeChunkEntity();
        chunkDelete.setDeleted(1);
        chunkDelete.setUpdatedAt(LocalDateTime.now());
        chunkMapper.update(chunkDelete, new LambdaQueryWrapper<KnowledgeChunkEntity>().in(KnowledgeChunkEntity::getDocumentId, documentIds));
        elementMapper.delete(new LambdaQueryWrapper<com.qingxu.qingxuapi.infrastructure.persistence.entity.KnowledgeElementEntity>()
                .in(com.qingxu.qingxuapi.infrastructure.persistence.entity.KnowledgeElementEntity::getDocumentId, documentIds));
        entityMentionMapper.delete(new LambdaQueryWrapper<KgEntityMentionEntity>().in(KgEntityMentionEntity::getDocumentId, documentIds));
        relationEvidenceMapper.delete(new LambdaQueryWrapper<KgRelationEvidenceEntity>().in(KgRelationEvidenceEntity::getDocumentId, documentIds));
        jobMapper.delete(new LambdaQueryWrapper<KnowledgeIndexJobEntity>().in(KnowledgeIndexJobEntity::getDocumentId, documentIds));
        extractionJobMapper.delete(new LambdaQueryWrapper<com.qingxu.qingxuapi.infrastructure.persistence.entity.KgExtractionJobEntity>()
                .in(com.qingxu.qingxuapi.infrastructure.persistence.entity.KgExtractionJobEntity::getDocumentId, documentIds));
    }

    private void cleanupGraphData(Long knowledgeId) {
        List<Long> entityIds = entityMapper.selectList(new LambdaQueryWrapper<com.qingxu.qingxuapi.infrastructure.persistence.entity.KgEntityEntity>()
                        .eq(com.qingxu.qingxuapi.infrastructure.persistence.entity.KgEntityEntity::getKnowledgeBaseId, knowledgeId))
                .stream()
                .map(com.qingxu.qingxuapi.infrastructure.persistence.entity.KgEntityEntity::getId)
                .toList();
        if (!entityIds.isEmpty()) {
            aliasMapper.delete(new LambdaQueryWrapper<com.qingxu.qingxuapi.infrastructure.persistence.entity.KgAliasEntity>()
                    .in(com.qingxu.qingxuapi.infrastructure.persistence.entity.KgAliasEntity::getEntityId, entityIds));
        }
        entityMapper.delete(new LambdaQueryWrapper<com.qingxu.qingxuapi.infrastructure.persistence.entity.KgEntityEntity>()
                .eq(com.qingxu.qingxuapi.infrastructure.persistence.entity.KgEntityEntity::getKnowledgeBaseId, knowledgeId));
        relationMapper.delete(new LambdaQueryWrapper<com.qingxu.qingxuapi.infrastructure.persistence.entity.KgRelationEntity>()
                .eq(com.qingxu.qingxuapi.infrastructure.persistence.entity.KgRelationEntity::getKnowledgeBaseId, knowledgeId));
    }

    private boolean canRemoveFile(Long fileId, Long currentKnowledgeId) {
        Long references = documentMapper.selectCount(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getFileId, fileId)
                .ne(KnowledgeDocumentEntity::getKnowledgeBaseId, currentKnowledgeId)
                .eq(KnowledgeDocumentEntity::getDeleted, 0));
        return references == 0;
    }

    private void removeFile(Long fileId) {
        SysFileEntity file = fileMapper.selectById(fileId);
        if (file == null) {
            return;
        }
        fileMapper.deleteById(fileId);
        try {
            fileStorageService.delete(file.getStoragePath());
        } catch (IOException exception) {
            log.warn("[knowledge-v2] failed to delete physical file: fileId={}", fileId, exception);
        }
    }

    private void storeChunksIfPresent(KnowledgeV2ProgressCallbackRequest request,
                                      KnowledgeIndexJobEntity job,
                                      KnowledgeDocumentEntity document,
                                      LocalDateTime now) {
        List<KnowledgeIndexedChunkRequest> chunks = request.chunks();
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        KnowledgeChunkEntity deleteMarker = new KnowledgeChunkEntity();
        deleteMarker.setDeleted(1);
        deleteMarker.setUpdatedAt(now);
        chunkMapper.update(deleteMarker, new LambdaQueryWrapper<KnowledgeChunkEntity>()
                .eq(KnowledgeChunkEntity::getDocumentId, document.getId())
                .eq(KnowledgeChunkEntity::getDeleted, 0));
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeIndexedChunkRequest chunk = chunks.get(i);
            KnowledgeChunkEntity entity = new KnowledgeChunkEntity();
            entity.setKnowledgeBaseId(job.getKnowledgeBaseId());
            entity.setDocumentId(document.getId());
            entity.setFileId(document.getFileId());
            entity.setChunkIndex(chunk.chunkIndex() == null ? i : chunk.chunkIndex());
            entity.setContent(chunk.content());
            entity.setContentType(defaultString(chunk.contentType(), "TEXT"));
            entity.setTitlePath(toJson(chunk.titlePath()));
            entity.setPageNumber(chunk.pageNumber());
            entity.setSheetName(chunk.sheetName());
            entity.setSlideNumber(chunk.slideNumber());
            entity.setStartElementIndex(chunk.startElementIndex());
            entity.setEndElementIndex(chunk.endElementIndex());
            entity.setTokenCount(chunk.tokenCount());
            entity.setChunkHash(defaultString(chunk.chunkHash(), ""));
            entity.setMetadataJson(toJson(chunk.metadata()));
            entity.setEmbeddingModel(chunk.embeddingModel());
            entity.setEmbeddingDimension(chunk.embeddingDimension());
            entity.setEmbeddingJson(toJson(chunk.embedding()));
            entity.setEmbeddingStatus(chunk.embedding() == null || chunk.embedding().isEmpty() ? "PENDING" : "SUCCESS");
            entity.setCreatedBy(document.getCreatedBy());
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            entity.setDeleted(0);
            chunkMapper.insert(entity);
        }
    }

    private String toJobStatus(String documentStatus) {
        if (KnowledgeDocumentStatus.INDEXED.name().equals(documentStatus) || KnowledgeIndexStage.DONE.name().equals(documentStatus)) {
            return KnowledgeIndexJobStatus.SUCCESS.name();
        }
        if (KnowledgeDocumentStatus.FAILED.name().equals(documentStatus)) {
            return KnowledgeIndexJobStatus.FAILED.name();
        }
        if (KnowledgeIndexJobStatus.CANCELED.name().equals(documentStatus)) {
            return KnowledgeIndexJobStatus.CANCELED.name();
        }
        if (KnowledgeDocumentStatus.PARSING.name().equals(documentStatus)
                || KnowledgeDocumentStatus.NORMALIZING.name().equals(documentStatus)
                || KnowledgeDocumentStatus.STRUCTURING.name().equals(documentStatus)
                || KnowledgeDocumentStatus.CHUNKING.name().equals(documentStatus)
                || KnowledgeDocumentStatus.EMBEDDING.name().equals(documentStatus)
                || KnowledgeDocumentStatus.STORING.name().equals(documentStatus)) {
            return KnowledgeIndexJobStatus.RUNNING.name();
        }
        return KnowledgeIndexJobStatus.RUNNING.name();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to serialize knowledge callback payload", exception);
        }
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String defaultVisibility(String visibility) {
        return visibility == null || visibility.isBlank() ? "PRIVATE" : visibility.trim().toUpperCase(Locale.ROOT);
    }

    private List<KnowledgeV2AttachmentRequest> safeAttachments(List<KnowledgeV2AttachmentRequest> attachments) {
        return attachments == null ? List.of() : attachments;
    }
}