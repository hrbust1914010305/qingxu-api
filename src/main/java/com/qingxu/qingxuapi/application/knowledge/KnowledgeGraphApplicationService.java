package com.qingxu.qingxuapi.application.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingxu.qingxuapi.common.exception.BusinessException;
import com.qingxu.qingxuapi.common.response.ErrorCode;
import com.qingxu.qingxuapi.domain.knowledge.KnowledgeDocumentStatus;
import com.qingxu.qingxuapi.domain.knowledge.KnowledgeGraphStatus;
import com.qingxu.qingxuapi.infrastructure.ai.KnowledgeAiClient;
import com.qingxu.qingxuapi.infrastructure.ai.KnowledgeGraphRuntimeStatusResponse;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KgEntityEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KgExtractionJobEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KgEntityMentionEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KgRelationEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KgRelationEvidenceEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KgAliasEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KnowledgeChunkEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KnowledgeBaseEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KgAliasMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KgEntityMentionMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KgEntityMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KgExtractionJobMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KgRelationEvidenceMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KgRelationMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KnowledgeBaseMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KnowledgeChunkMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KnowledgeDocumentMapper;
import com.qingxu.qingxuapi.interfaces.auth.dto.CurrentUserResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.dto.KnowledgeGraphEdgeResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.dto.KnowledgeGraphEntityDetailResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.dto.KnowledgeGraphNodeResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.dto.KnowledgeGraphResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.dto.KnowledgeGraphRelationDetailResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.dto.KnowledgeGraphRelationEvidenceResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.dto.KnowledgeGraphSourceChunkResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2GraphResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class KnowledgeGraphApplicationService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final KgEntityMapper entityMapper;
    private final KgRelationMapper relationMapper;
    private final KgEntityMentionMapper entityMentionMapper;
    private final KgRelationEvidenceMapper relationEvidenceMapper;
    private final KgAliasMapper aliasMapper;
    private final KgExtractionJobMapper extractionJobMapper;
    private final KnowledgeGraphBuildApplicationService graphBuildService;
    private final KnowledgeAiClient aiClient;

    private static final Set<String> RUNNING_GRAPH_JOB_STATUSES = Set.of("PENDING", "RUNNING");
    private static final long GRAPH_JOB_START_GRACE_SECONDS = 5;

    public KnowledgeGraphResponse graph(Long knowledgeBaseId,
                                        List<String> entityTypes,
                                        List<String> relationTypes,
                                        Long fileId,
                                        String keyword,
                                        Integer depth,
                                        Integer limit,
                                        CurrentUserResponse currentUser) {
        KnowledgeBaseEntity knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null || !currentUser.id().equals(knowledgeBase.getCreatedBy())) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }
        int resolvedLimit = limit == null || limit <= 0 || limit > 500 ? 200 : limit;
        LambdaQueryWrapper<KgEntityEntity> entityQuery = new LambdaQueryWrapper<KgEntityEntity>()
                .eq(KgEntityEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KgEntityEntity::getDeleted, 0)
                .last("limit " + resolvedLimit);
        if (entityTypes != null && !entityTypes.isEmpty()) {
            entityQuery.in(KgEntityEntity::getEntityType, entityTypes);
        }
        if (keyword != null && !keyword.isBlank()) {
            entityQuery.and(q -> q.like(KgEntityEntity::getName, keyword.trim())
                    .or()
                    .like(KgEntityEntity::getDescription, keyword.trim()));
        }
        if (fileId != null) {
            List<Long> sourceEntityIds = entityMentionMapper.selectList(new LambdaQueryWrapper<KgEntityMentionEntity>()
                            .eq(KgEntityMentionEntity::getFileId, fileId))
                    .stream()
                    .map(KgEntityMentionEntity::getEntityId)
                    .distinct()
                    .toList();
            if (sourceEntityIds.isEmpty()) {
                return new KnowledgeGraphResponse(List.of(), List.of());
            }
            entityQuery.in(KgEntityEntity::getId, sourceEntityIds);
        }
        List<KgEntityEntity> entities = entityMapper.selectList(entityQuery);
        List<Long> entityIds = entities.stream().map(KgEntityEntity::getId).toList();
        if (entityIds.isEmpty()) {
            return new KnowledgeGraphResponse(List.of(), List.of());
        }

        LambdaQueryWrapper<KgRelationEntity> relationQuery = new LambdaQueryWrapper<KgRelationEntity>()
                .eq(KgRelationEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KgRelationEntity::getDeleted, 0)
                .last("limit " + resolvedLimit);
        relationQuery.in(KgRelationEntity::getSourceEntityId, entityIds)
                .in(KgRelationEntity::getTargetEntityId, entityIds);
        if (relationTypes != null && !relationTypes.isEmpty()) {
            relationQuery.in(KgRelationEntity::getRelationType, relationTypes);
        }
        if (fileId != null) {
            List<Long> documentIds = documentMapper.selectList(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                            .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
                            .eq(KnowledgeDocumentEntity::getFileId, fileId))
                    .stream()
                    .map(KnowledgeDocumentEntity::getId)
                    .toList();
            if (documentIds.isEmpty()) {
                return new KnowledgeGraphResponse(entities.stream().map(this::toNode).toList(), List.of());
            }
            List<Long> relationIds = relationEvidenceMapper.selectList(new LambdaQueryWrapper<KgRelationEvidenceEntity>()
                            .in(KgRelationEvidenceEntity::getDocumentId, documentIds))
                    .stream()
                    .map(KgRelationEvidenceEntity::getRelationId)
                    .distinct()
                    .toList();
            if (relationIds.isEmpty()) {
                return new KnowledgeGraphResponse(entities.stream().map(this::toNode).toList(), List.of());
            }
            relationQuery.in(KgRelationEntity::getId, relationIds);
        }
        List<KgRelationEntity> relations = relationMapper.selectList(relationQuery);

        return new KnowledgeGraphResponse(
                entities.stream().map(this::toNode).toList(),
                relations.stream().map(this::toEdge).toList()
        );
    }

    public KnowledgeV2GraphResponse graphWithStatus(Long knowledgeBaseId,
                                                    List<String> entityTypes,
                                                    List<String> relationTypes,
                                                    Long fileId,
                                                    String keyword,
                                                    Integer depth,
                                                    Integer limit,
                                                    CurrentUserResponse currentUser) {
        KnowledgeBaseEntity knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null || Integer.valueOf(1).equals(knowledgeBase.getDeleted()) || !currentUser.id().equals(knowledgeBase.getCreatedBy())) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }

        List<KnowledgeDocumentEntity> indexedDocuments = indexedDocuments(knowledgeBaseId, fileId);
        if (indexedDocuments.isEmpty()) {
            return new KnowledgeV2GraphResponse(
                    knowledgeBaseId,
                    KnowledgeGraphStatus.NOT_STARTED.name(),
                    false,
                    0,
                    "请先完成知识库解析",
                    null,
                    List.of(),
                    List.of()
            );
        }

        GraphBuildState beforeStart = graphBuildState(indexedDocuments);
        if (!beforeStart.ready() && !beforeStart.running() && !beforeStart.failed()) {
            graphBuildService.startForIndexedDocuments(knowledgeBaseId, fileId);
        }

        GraphBuildState state = graphBuildState(indexedDocuments);
        KnowledgeGraphResponse graph = state.ready()
                ? graph(knowledgeBaseId, entityTypes, relationTypes, fileId, keyword, depth, limit, currentUser)
                : new KnowledgeGraphResponse(List.of(), List.of());

        KnowledgeGraphStatus status = resolveGraphStatus(state);
        return new KnowledgeV2GraphResponse(
                knowledgeBaseId,
                status.name(),
                state.ready(),
                state.progress(),
                graphMessage(status, state),
                state.errorMessage(),
                graph.nodes(),
                graph.edges()
        );
    }

    public KnowledgeV2GraphResponse regenerateGraph(Long knowledgeBaseId, Long fileId, CurrentUserResponse currentUser) {
        KnowledgeBaseEntity knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null || Integer.valueOf(1).equals(knowledgeBase.getDeleted()) || !currentUser.id().equals(knowledgeBase.getCreatedBy())) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }
        List<KnowledgeDocumentEntity> indexedDocuments = indexedDocuments(knowledgeBaseId, fileId);
        if (indexedDocuments.isEmpty()) {
            return new KnowledgeV2GraphResponse(
                    knowledgeBaseId,
                    KnowledgeGraphStatus.NOT_STARTED.name(),
                    false,
                    0,
                    "请先完成知识库解析",
                    null,
                    List.of(),
                    List.of()
            );
        }

        cleanupGraphForRegenerate(knowledgeBaseId, indexedDocuments, fileId == null);
        graphBuildService.startForIndexedDocuments(knowledgeBaseId, fileId);
        GraphBuildState state = graphBuildState(indexedDocuments);
        return new KnowledgeV2GraphResponse(
                knowledgeBaseId,
                state.running() ? KnowledgeGraphStatus.GENERATING.name() : KnowledgeGraphStatus.NOT_STARTED.name(),
                false,
                state.progress(),
                state.running() ? "图谱重新生成中" : "图谱重新生成任务已提交，等待AI启动",
                state.errorMessage(),
                List.of(),
                List.of()
        );
    }

    public KnowledgeGraphEntityDetailResponse entityDetail(Long entityId, CurrentUserResponse currentUser) {
        KgEntityEntity entity = entityMapper.selectById(entityId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        ensureKnowledgeBaseOwner(entity.getKnowledgeBaseId(), currentUser);
        List<String> aliases = aliasMapper.selectList(new LambdaQueryWrapper<KgAliasEntity>()
                        .eq(KgAliasEntity::getEntityId, entityId))
                .stream()
                .map(KgAliasEntity::getAlias)
                .toList();
        List<KnowledgeGraphSourceChunkResponse> sourceChunks = entityMentionMapper.selectList(new LambdaQueryWrapper<KgEntityMentionEntity>()
                        .eq(KgEntityMentionEntity::getEntityId, entityId))
                .stream()
                .map(this::toSourceChunk)
                .toList();
        return new KnowledgeGraphEntityDetailResponse(
                entity.getId(),
                entity.getKnowledgeBaseId(),
                entity.getEntityType(),
                entity.getName(),
                entity.getNormalizedName(),
                entity.getDescription(),
                entity.getConfidence(),
                aliases,
                sourceChunks
        );
    }

    public KnowledgeGraphRelationDetailResponse relationDetail(Long relationId, CurrentUserResponse currentUser) {
        KgRelationEntity relation = relationMapper.selectById(relationId);
        if (relation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        ensureKnowledgeBaseOwner(relation.getKnowledgeBaseId(), currentUser);
        KgEntityEntity source = entityMapper.selectById(relation.getSourceEntityId());
        KgEntityEntity target = entityMapper.selectById(relation.getTargetEntityId());
        List<KnowledgeGraphRelationEvidenceResponse> evidences = relationEvidenceMapper.selectList(new LambdaQueryWrapper<KgRelationEvidenceEntity>()
                        .eq(KgRelationEvidenceEntity::getRelationId, relationId))
                .stream()
                .map(this::toEvidence)
                .toList();
        return new KnowledgeGraphRelationDetailResponse(
                relation.getId(),
                relation.getKnowledgeBaseId(),
                source == null ? null : toNode(source),
                target == null ? null : toNode(target),
                relation.getRelationType(),
                relation.getRelationLabelZh(),
                relation.getDescription(),
                relation.getConfidence(),
                evidences
        );
    }

    private void ensureKnowledgeBaseOwner(Long knowledgeBaseId, CurrentUserResponse currentUser) {
        KnowledgeBaseEntity knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null || !currentUser.id().equals(knowledgeBase.getCreatedBy())) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }
    }

    private KnowledgeGraphSourceChunkResponse toSourceChunk(KgEntityMentionEntity mention) {
        KnowledgeChunkEntity chunk = mention.getChunkId() == null ? null : chunkMapper.selectById(mention.getChunkId());
        return new KnowledgeGraphSourceChunkResponse(
                mention.getChunkId(),
                mention.getDocumentId(),
                mention.getFileId(),
                chunk == null ? null : chunk.getChunkIndex(),
                mention.getMentionText(),
                chunk == null ? null : chunk.getContent(),
                chunk == null ? mention.getTitlePath() : chunk.getTitlePath(),
                chunk == null ? mention.getPageNumber() : chunk.getPageNumber()
        );
    }

    private KnowledgeGraphRelationEvidenceResponse toEvidence(KgRelationEvidenceEntity evidence) {
        KnowledgeDocumentEntity document = documentMapper.selectById(evidence.getDocumentId());
        return new KnowledgeGraphRelationEvidenceResponse(
                evidence.getId(),
                evidence.getDocumentId(),
                document == null ? null : document.getFileId(),
                evidence.getChunkId(),
                evidence.getEvidenceText(),
                evidence.getConfidence()
        );
    }

    private KnowledgeGraphNodeResponse toNode(KgEntityEntity entity) {
        return new KnowledgeGraphNodeResponse(entity.getId(), entity.getEntityType(), entity.getName(), entity.getNormalizedName(), entity.getDescription(), entity.getConfidence());
    }

    private KnowledgeGraphEdgeResponse toEdge(KgRelationEntity relation) {
        return new KnowledgeGraphEdgeResponse(relation.getId(), relation.getSourceEntityId(), relation.getTargetEntityId(), relation.getRelationType(), relation.getRelationLabelZh(), relation.getDescription(), relation.getConfidence());
    }

    private void cleanupGraphForRegenerate(Long knowledgeBaseId, List<KnowledgeDocumentEntity> documents, boolean wholeKnowledgeBase) {
        List<Long> documentIds = documents.stream().map(KnowledgeDocumentEntity::getId).toList();
        if (documentIds.isEmpty()) {
            return;
        }
        for (KnowledgeDocumentEntity document : documents) {
            document.setGraphReady(false);
            documentMapper.updateById(document);
        }
        entityMentionMapper.delete(new LambdaQueryWrapper<KgEntityMentionEntity>()
                .in(KgEntityMentionEntity::getDocumentId, documentIds));
        relationEvidenceMapper.delete(new LambdaQueryWrapper<KgRelationEvidenceEntity>()
                .in(KgRelationEvidenceEntity::getDocumentId, documentIds));
        extractionJobMapper.delete(new LambdaQueryWrapper<KgExtractionJobEntity>()
                .in(KgExtractionJobEntity::getDocumentId, documentIds));
        if (wholeKnowledgeBase) {
            cleanupKnowledgeGraph(knowledgeBaseId);
        } else {
            cleanupOrphanGraphData(knowledgeBaseId);
        }
    }

    private void cleanupKnowledgeGraph(Long knowledgeBaseId) {
        List<Long> entityIds = entityMapper.selectList(new LambdaQueryWrapper<KgEntityEntity>()
                        .eq(KgEntityEntity::getKnowledgeBaseId, knowledgeBaseId))
                .stream()
                .map(KgEntityEntity::getId)
                .toList();
        if (!entityIds.isEmpty()) {
            aliasMapper.delete(new LambdaQueryWrapper<KgAliasEntity>()
                    .in(KgAliasEntity::getEntityId, entityIds));
        }
        entityMapper.delete(new LambdaQueryWrapper<KgEntityEntity>()
                .eq(KgEntityEntity::getKnowledgeBaseId, knowledgeBaseId));
        relationMapper.delete(new LambdaQueryWrapper<KgRelationEntity>()
                .eq(KgRelationEntity::getKnowledgeBaseId, knowledgeBaseId));
    }

    private void cleanupOrphanGraphData(Long knowledgeBaseId) {
        List<Long> relationIds = relationMapper.selectList(new LambdaQueryWrapper<KgRelationEntity>()
                        .eq(KgRelationEntity::getKnowledgeBaseId, knowledgeBaseId))
                .stream()
                .map(KgRelationEntity::getId)
                .toList();
        for (Long relationId : relationIds) {
            Long evidenceCount = relationEvidenceMapper.selectCount(new LambdaQueryWrapper<KgRelationEvidenceEntity>()
                    .eq(KgRelationEvidenceEntity::getRelationId, relationId));
            if (evidenceCount == null || evidenceCount == 0) {
                relationMapper.deleteById(relationId);
            }
        }

        List<KgEntityEntity> entities = entityMapper.selectList(new LambdaQueryWrapper<KgEntityEntity>()
                .eq(KgEntityEntity::getKnowledgeBaseId, knowledgeBaseId));
        for (KgEntityEntity entity : entities) {
            Long mentionCount = entityMentionMapper.selectCount(new LambdaQueryWrapper<KgEntityMentionEntity>()
                    .eq(KgEntityMentionEntity::getEntityId, entity.getId()));
            if (mentionCount != null && mentionCount > 0) {
                continue;
            }
            Long relationCount = relationMapper.selectCount(new LambdaQueryWrapper<KgRelationEntity>()
                    .eq(KgRelationEntity::getKnowledgeBaseId, knowledgeBaseId)
                    .and(query -> query.eq(KgRelationEntity::getSourceEntityId, entity.getId())
                            .or()
                            .eq(KgRelationEntity::getTargetEntityId, entity.getId())));
            if (relationCount == null || relationCount == 0) {
                aliasMapper.delete(new LambdaQueryWrapper<KgAliasEntity>()
                        .eq(KgAliasEntity::getEntityId, entity.getId()));
                entityMapper.deleteById(entity.getId());
            }
        }
    }

    private List<KnowledgeDocumentEntity> indexedDocuments(Long knowledgeBaseId, Long fileId) {
        LambdaQueryWrapper<KnowledgeDocumentEntity> wrapper = new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KnowledgeDocumentEntity::getDeleted, 0)
                .eq(KnowledgeDocumentEntity::getStatus, KnowledgeDocumentStatus.INDEXED.name());
        if (fileId != null) {
            wrapper.eq(KnowledgeDocumentEntity::getFileId, fileId);
        }
        return documentMapper.selectList(wrapper);
    }

    private GraphBuildState graphBuildState(List<KnowledgeDocumentEntity> documents) {
        int total = documents.size();
        int ready = 0;
        boolean running = false;
        boolean waiting = false;
        boolean failed = false;
        String errorMessage = null;
        for (KnowledgeDocumentEntity document : documents) {
            if (Boolean.TRUE.equals(document.getGraphReady())) {
                ready++;
                continue;
            }
            KgExtractionJobEntity job = latestGraphJob(document.getId());
            if (job == null) {
                continue;
            }
            if (RUNNING_GRAPH_JOB_STATUSES.contains(job.getStatus())) {
                if (isGraphJobReallyRunning(job)) {
                    running = true;
                } else if (isRecentGraphJob(job)) {
                    waiting = true;
                } else {
                    markGraphJobNotRunning(job);
                    failed = true;
                    if (errorMessage == null) {
                        errorMessage = job.getErrorMessage();
                    }
                }
            } else if ("FAILED".equals(job.getStatus())) {
                failed = true;
                if (errorMessage == null) {
                    errorMessage = job.getErrorMessage();
                }
            } else if ("SUCCESS".equals(job.getStatus())) {
                ready++;
            }
        }
        int progress = total == 0 ? 0 : (int) Math.round((ready * 100.0) / total);
        if (running && progress >= 100) {
            progress = 99;
        }
        return new GraphBuildState(total, ready, running, waiting, failed, progress, errorMessage);
    }

    private KgExtractionJobEntity latestGraphJob(Long documentId) {
        return extractionJobMapper.selectOne(new LambdaQueryWrapper<KgExtractionJobEntity>()
                .eq(KgExtractionJobEntity::getDocumentId, documentId)
                .orderByDesc(KgExtractionJobEntity::getId)
                .last("limit 1"));
    }

    private boolean isGraphJobReallyRunning(KgExtractionJobEntity job) {
        KnowledgeGraphRuntimeStatusResponse runtimeStatus = aiClient.graphRuntimeStatus(job.getId());
        return runtimeStatus != null && Boolean.TRUE.equals(runtimeStatus.running());
    }

    private boolean isRecentGraphJob(KgExtractionJobEntity job) {
        LocalDateTime updatedAt = job.getUpdatedAt() == null ? job.getCreatedAt() : job.getUpdatedAt();
        return updatedAt != null && updatedAt.isAfter(LocalDateTime.now().minusSeconds(GRAPH_JOB_START_GRACE_SECONDS));
    }

    private void markGraphJobNotRunning(KgExtractionJobEntity job) {
        if (job == null || job.getId() == null || !RUNNING_GRAPH_JOB_STATUSES.contains(job.getStatus())) {
            return;
        }
        job.setStatus("FAILED");
        job.setErrorMessage("图谱任务未在AI服务运行");
        extractionJobMapper.updateById(job);
    }

    private KnowledgeGraphStatus resolveGraphStatus(GraphBuildState state) {
        if (state.ready()) {
            return KnowledgeGraphStatus.READY;
        }
        if (state.running()) {
            return KnowledgeGraphStatus.GENERATING;
        }
        if (state.failed()) {
            return KnowledgeGraphStatus.FAILED;
        }
        return KnowledgeGraphStatus.NOT_STARTED;
    }

    private String graphMessage(KnowledgeGraphStatus status, GraphBuildState state) {
        return switch (status) {
            case READY -> "图谱已生成";
            case GENERATING -> "图谱生成中";
            case FAILED -> "图谱生成失败";
            case NOT_STARTED -> state.waiting() ? "图谱任务已提交，等待AI启动" : "图谱未生成";
        };
    }

    private record GraphBuildState(
            int total,
            int readyCount,
            boolean running,
            boolean waiting,
            boolean failed,
            int progress,
            String errorMessage
    ) {
        boolean ready() {
            return total > 0 && readyCount >= total;
        }
    }
}
