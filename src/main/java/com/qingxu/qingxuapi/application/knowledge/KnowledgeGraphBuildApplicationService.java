package com.qingxu.qingxuapi.application.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingxu.qingxuapi.domain.knowledge.KnowledgeIndexJobStatus;
import com.qingxu.qingxuapi.domain.knowledge.KnowledgeIndexStage;
import com.qingxu.qingxuapi.infrastructure.ai.KnowledgeAiClient;
import com.qingxu.qingxuapi.infrastructure.ai.KnowledgeGraphBuildChunkRequest;
import com.qingxu.qingxuapi.infrastructure.ai.KnowledgeGraphBuildStartRequest;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KgAliasEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KgEntityEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KgEntityMentionEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KgExtractionJobEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KgRelationEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KgRelationEvidenceEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KnowledgeBaseEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KnowledgeChunkEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.KnowledgeIndexJobEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KgAliasMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KgEntityMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KgEntityMentionMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KgExtractionJobMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KgRelationEvidenceMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KgRelationMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KnowledgeBaseMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KnowledgeChunkMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KnowledgeDocumentMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.KnowledgeIndexJobMapper;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeGraphEntityMentionRequest;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeGraphEntityRequest;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeGraphProgressCallbackRequest;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeGraphRelationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphBuildApplicationService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private final KnowledgeChunkMapper chunkMapper;
    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeIndexJobMapper indexJobMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KgExtractionJobMapper extractionJobMapper;
    private final KgEntityMapper entityMapper;
    private final KgAliasMapper aliasMapper;
    private final KgEntityMentionMapper entityMentionMapper;
    private final KgRelationMapper relationMapper;
    private final KgRelationEvidenceMapper relationEvidenceMapper;
    private final KnowledgeAiClient aiClient;

    @Transactional
    public void startForIndexedDocuments(Long knowledgeBaseId) {
        startForIndexedDocuments(knowledgeBaseId, null);
    }

    @Transactional
    public void startForIndexedDocuments(Long knowledgeBaseId, Long fileId) {
        LambdaQueryWrapper<KnowledgeDocumentEntity> wrapper = new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KnowledgeDocumentEntity::getDeleted, 0)
                .eq(KnowledgeDocumentEntity::getStatus, "INDEXED");
        if (fileId != null) {
            wrapper.eq(KnowledgeDocumentEntity::getFileId, fileId);
        }
        List<KnowledgeDocumentEntity> documents = documentMapper.selectList(wrapper);
        for (KnowledgeDocumentEntity document : documents) {
            if (Boolean.TRUE.equals(document.getGraphReady())) {
                continue;
            }
            KnowledgeIndexJobEntity indexJob = latestSuccessfulIndexJob(document.getId());
            if (indexJob != null) {
                startForIndexedDocument(indexJob, document);
            }
        }
    }

    @Transactional
    public void startForIndexedDocument(KnowledgeIndexJobEntity indexJob, KnowledgeDocumentEntity document) {
        if (indexJob == null || document == null || !KnowledgeIndexJobStatus.SUCCESS.name().equals(indexJob.getStatus())) {
            return;
        }
        if (Boolean.TRUE.equals(document.getGraphReady())) {
            return;
        }
        KgExtractionJobEntity existing = latestGraphJob(document.getId());
        if (existing != null && (STATUS_PENDING.equals(existing.getStatus()) || STATUS_RUNNING.equals(existing.getStatus()) || STATUS_SUCCESS.equals(existing.getStatus()))) {
            return;
        }
        List<KnowledgeChunkEntity> chunks = chunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunkEntity>()
                .eq(KnowledgeChunkEntity::getDocumentId, document.getId())
                .eq(KnowledgeChunkEntity::getDeleted, 0)
                .orderByAsc(KnowledgeChunkEntity::getChunkIndex));
        if (chunks.isEmpty()) {
            log.info("[knowledge-graph] skip graph build because document has no chunks: documentId={}", document.getId());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        KgExtractionJobEntity graphJob = new KgExtractionJobEntity();
        graphJob.setJobId(indexJob.getId());
        graphJob.setDocumentId(document.getId());
        graphJob.setKnowledgeBaseId(document.getKnowledgeBaseId());
        graphJob.setStatus(STATUS_PENDING);
        graphJob.setCreatedAt(now);
        graphJob.setUpdatedAt(now);
        extractionJobMapper.insert(graphJob);

        try {
            aiClient.startGraphBuild(new KnowledgeGraphBuildStartRequest(
                    graphJob.getId(),
                    indexJob.getId(),
                    document.getKnowledgeBaseId(),
                    document.getId(),
                    document.getFileId(),
                    document.getCreatedBy(),
                    resolveGraphDomain(document.getKnowledgeBaseId()),
                    chunks.stream().map(this::toGraphChunk).toList()
            ));
            graphJob.setStatus(STATUS_RUNNING);
            graphJob.setUpdatedAt(LocalDateTime.now());
            extractionJobMapper.updateById(graphJob);
        } catch (RuntimeException exception) {
            graphJob.setStatus(STATUS_FAILED);
            graphJob.setErrorMessage(exception.getMessage());
            graphJob.setUpdatedAt(LocalDateTime.now());
            extractionJobMapper.updateById(graphJob);
            log.warn("[knowledge-graph] graph build start failed: graphJobId={}, documentId={}, message={}",
                    graphJob.getId(), document.getId(), exception.getMessage(), exception);
        }
    }

    @Transactional
    public void reportProgress(KnowledgeGraphProgressCallbackRequest request) {
        KgExtractionJobEntity graphJob = extractionJobMapper.selectById(request.graphJobId());
        if (graphJob == null) {
            log.warn("[knowledge-graph] ignore graph callback because job does not exist: graphJobId={}", request.graphJobId());
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        graphJob.setStatus(toGraphJobStatus(request.status(), request.stage()));
        graphJob.setErrorMessage(request.errorMessage());
        graphJob.setUpdatedAt(now);

        if (STATUS_SUCCESS.equals(graphJob.getStatus())) {
            storeGraph(request, graphJob, now);
            KnowledgeDocumentEntity document = documentMapper.selectById(graphJob.getDocumentId());
            if (document != null) {
                document.setGraphReady(true);
                document.setUpdatedAt(now);
                documentMapper.updateById(document);
            }
        }
        extractionJobMapper.updateById(graphJob);
    }

    private void storeGraph(KnowledgeGraphProgressCallbackRequest request, KgExtractionJobEntity graphJob, LocalDateTime now) {
        cleanupDocumentGraphEvidence(graphJob.getDocumentId());
        cleanupOrphanGraphData(graphJob.getKnowledgeBaseId());
        Map<String, KgEntityEntity> entityByKey = new HashMap<>();
        for (KnowledgeGraphEntityRequest entityRequest : safeList(request.entities())) {
            if (entityRequest == null) {
                continue;
            }
            KgEntityEntity entity = findOrCreateEntity(graphJob, entityRequest, now);
            entityByKey.put(entityKey(entity.getEntityType(), entity.getNormalizedName()), entity);
            storeAliases(entity, entityRequest, now);
            storeMentions(entity, graphJob, entityRequest, now);
        }
        for (KnowledgeGraphRelationRequest relationRequest : safeList(request.relations())) {
            if (relationRequest == null) {
                continue;
            }
            KgEntityEntity source = findEntityForRelation(graphJob, relationRequest.sourceType(), relationRequest.sourceName(), entityByKey);
            KgEntityEntity target = findEntityForRelation(graphJob, relationRequest.targetType(), relationRequest.targetName(), entityByKey);
            if (source == null || target == null) {
                log.warn("[knowledge-graph] skip relation because source or target is missing: graphJobId={}, source={}, target={}",
                        graphJob.getId(), relationRequest.sourceName(), relationRequest.targetName());
                continue;
            }
            KgRelationEntity relation = findOrCreateRelation(graphJob, source, target, relationRequest, now);
            storeRelationEvidence(relation, graphJob, relationRequest, now);
        }
    }

    private KgEntityEntity findOrCreateEntity(KgExtractionJobEntity graphJob, KnowledgeGraphEntityRequest request, LocalDateTime now) {
        String type = defaultString(request.entityType(), "OTHER");
        String name = defaultString(request.name(), "未命名实体");
        String normalizedName = normalizeName(name);
        KgEntityEntity existing = entityMapper.selectOne(new LambdaQueryWrapper<KgEntityEntity>()
                .eq(KgEntityEntity::getKnowledgeBaseId, graphJob.getKnowledgeBaseId())
                .eq(KgEntityEntity::getEntityType, type)
                .eq(KgEntityEntity::getNormalizedName, normalizedName)
                .eq(KgEntityEntity::getDeleted, 0)
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }
        KgEntityEntity entity = new KgEntityEntity();
        entity.setKnowledgeBaseId(graphJob.getKnowledgeBaseId());
        entity.setEntityType(type);
        entity.setName(name);
        entity.setNormalizedName(normalizedName);
        entity.setDescription(request.description());
        entity.setConfidence(defaultConfidence(request.confidence()));
        entity.setCreatedBy(resolveDocumentCreatedBy(graphJob.getDocumentId()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setDeleted(0);
        entityMapper.insert(entity);
        return entity;
    }

    private void storeAliases(KgEntityEntity entity, KnowledgeGraphEntityRequest request, LocalDateTime now) {
        String llmNormalizedName = defaultString(request.normalizedName(), null);
        if (llmNormalizedName != null && !normalizeName(llmNormalizedName).equals(entity.getNormalizedName())) {
            storeAlias(entity, llmNormalizedName, request.confidence(), now);
        }
        for (String aliasValue : safeList(request.aliases())) {
            if (aliasValue == null || aliasValue.isBlank()) {
                continue;
            }
            storeAlias(entity, aliasValue, request.confidence(), now);
        }
    }

    private void storeAlias(KgEntityEntity entity, String aliasValue, BigDecimal confidence, LocalDateTime now) {
        if (aliasValue == null || aliasValue.isBlank()) {
            return;
        }
        String aliasText = aliasValue.trim();
        Long existingCount = aliasMapper.selectCount(new LambdaQueryWrapper<KgAliasEntity>()
                .eq(KgAliasEntity::getEntityId, entity.getId())
                .eq(KgAliasEntity::getAlias, aliasText));
        if (existingCount != null && existingCount > 0) {
            return;
        }
        KgAliasEntity alias = new KgAliasEntity();
        alias.setEntityId(entity.getId());
        alias.setAlias(aliasText);
        alias.setSource("LLM");
        alias.setConfidence(defaultConfidence(confidence));
        alias.setCreatedAt(now);
        aliasMapper.insert(alias);
    }

    private void storeMentions(KgEntityEntity entity, KgExtractionJobEntity graphJob, KnowledgeGraphEntityRequest request, LocalDateTime now) {
        for (KnowledgeGraphEntityMentionRequest mentionRequest : safeList(request.mentions())) {
            if (mentionRequest.mentionText() == null || mentionRequest.mentionText().isBlank()) {
                continue;
            }
            KnowledgeChunkEntity chunk = mentionRequest.chunkId() == null ? null : chunkMapper.selectById(mentionRequest.chunkId());
            KgEntityMentionEntity mention = new KgEntityMentionEntity();
            mention.setEntityId(entity.getId());
            mention.setDocumentId(graphJob.getDocumentId());
            mention.setChunkId(mentionRequest.chunkId());
            mention.setFileId(chunk == null ? null : chunk.getFileId());
            mention.setMentionText(mentionRequest.mentionText().trim());
            mention.setTitlePath(defaultString(mentionRequest.titlePath(), chunk == null ? null : chunk.getTitlePath()));
            mention.setPageNumber(mentionRequest.pageNumber() == null && chunk != null ? chunk.getPageNumber() : mentionRequest.pageNumber());
            mention.setStartOffset(mentionRequest.startOffset());
            mention.setEndOffset(mentionRequest.endOffset());
            mention.setConfidence(defaultConfidence(mentionRequest.confidence()));
            mention.setCreatedAt(now);
            if (mention.getFileId() != null) {
                entityMentionMapper.insert(mention);
            }
        }
    }

    private KgEntityEntity findEntityForRelation(KgExtractionJobEntity graphJob, String type, String name, Map<String, KgEntityEntity> entityByKey) {
        String normalizedName = normalizeName(name);
        String resolvedType = defaultString(type, "OTHER");
        KgEntityEntity entity = entityByKey.get(entityKey(resolvedType, normalizedName));
        if (entity != null) {
            return entity;
        }
        return entityMapper.selectOne(new LambdaQueryWrapper<KgEntityEntity>()
                .eq(KgEntityEntity::getKnowledgeBaseId, graphJob.getKnowledgeBaseId())
                .eq(KgEntityEntity::getEntityType, resolvedType)
                .eq(KgEntityEntity::getNormalizedName, normalizedName)
                .eq(KgEntityEntity::getDeleted, 0)
                .last("limit 1"));
    }

    private KgRelationEntity findOrCreateRelation(KgExtractionJobEntity graphJob,
                                                  KgEntityEntity source,
                                                  KgEntityEntity target,
                                                  KnowledgeGraphRelationRequest request,
                                                  LocalDateTime now) {
        String relationType = defaultString(request.relationType(), "RELATED_TO");
        String relationLabelZh = blankToNull(request.relationLabelZh());
        KgRelationEntity existing = relationMapper.selectOne(new LambdaQueryWrapper<KgRelationEntity>()
                .eq(KgRelationEntity::getKnowledgeBaseId, graphJob.getKnowledgeBaseId())
                .eq(KgRelationEntity::getSourceEntityId, source.getId())
                .eq(KgRelationEntity::getTargetEntityId, target.getId())
                .eq(KgRelationEntity::getRelationType, relationType)
                .eq(relationLabelZh != null, KgRelationEntity::getRelationLabelZh, relationLabelZh)
                .isNull(relationLabelZh == null, KgRelationEntity::getRelationLabelZh)
                .eq(KgRelationEntity::getDeleted, 0)
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }
        KgRelationEntity relation = new KgRelationEntity();
        relation.setKnowledgeBaseId(graphJob.getKnowledgeBaseId());
        relation.setSourceEntityId(source.getId());
        relation.setTargetEntityId(target.getId());
        relation.setRelationType(relationType);
        relation.setRelationLabelZh(relationLabelZh);
        relation.setDescription(request.description());
        relation.setConfidence(defaultConfidence(request.confidence()));
        relation.setCreatedAt(now);
        relation.setUpdatedAt(now);
        relation.setDeleted(0);
        relationMapper.insert(relation);
        return relation;
    }

    private void storeRelationEvidence(KgRelationEntity relation, KgExtractionJobEntity graphJob, KnowledgeGraphRelationRequest request, LocalDateTime now) {
        if (request.chunkId() == null || request.evidenceText() == null || request.evidenceText().isBlank()) {
            return;
        }
        KgRelationEvidenceEntity evidence = new KgRelationEvidenceEntity();
        evidence.setRelationId(relation.getId());
        evidence.setDocumentId(graphJob.getDocumentId());
        evidence.setChunkId(request.chunkId());
        evidence.setEvidenceText(request.evidenceText());
        evidence.setConfidence(defaultConfidence(request.confidence()));
        evidence.setCreatedAt(now);
        relationEvidenceMapper.insert(evidence);
    }

    private void cleanupDocumentGraphEvidence(Long documentId) {
        entityMentionMapper.delete(new LambdaQueryWrapper<KgEntityMentionEntity>()
                .eq(KgEntityMentionEntity::getDocumentId, documentId));
        relationEvidenceMapper.delete(new LambdaQueryWrapper<KgRelationEvidenceEntity>()
                .eq(KgRelationEvidenceEntity::getDocumentId, documentId));
    }

    private void cleanupOrphanGraphData(Long knowledgeBaseId) {
        List<Long> relationIds = relationMapper.selectList(new LambdaQueryWrapper<KgRelationEntity>()
                        .eq(KgRelationEntity::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(KgRelationEntity::getDeleted, 0))
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
                .eq(KgEntityEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KgEntityEntity::getDeleted, 0));
        for (KgEntityEntity entity : entities) {
            Long mentionCount = entityMentionMapper.selectCount(new LambdaQueryWrapper<KgEntityMentionEntity>()
                    .eq(KgEntityMentionEntity::getEntityId, entity.getId()));
            if (mentionCount != null && mentionCount > 0) {
                continue;
            }
            Long relationCount = relationMapper.selectCount(new LambdaQueryWrapper<KgRelationEntity>()
                    .eq(KgRelationEntity::getKnowledgeBaseId, knowledgeBaseId)
                    .eq(KgRelationEntity::getDeleted, 0)
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

    private KgExtractionJobEntity latestGraphJob(Long documentId) {
        return extractionJobMapper.selectOne(new LambdaQueryWrapper<KgExtractionJobEntity>()
                .eq(KgExtractionJobEntity::getDocumentId, documentId)
                .orderByDesc(KgExtractionJobEntity::getId)
                .last("limit 1"));
    }

    private KnowledgeIndexJobEntity latestSuccessfulIndexJob(Long documentId) {
        return indexJobMapper.selectOne(new LambdaQueryWrapper<KnowledgeIndexJobEntity>()
                .eq(KnowledgeIndexJobEntity::getDocumentId, documentId)
                .eq(KnowledgeIndexJobEntity::getStatus, KnowledgeIndexJobStatus.SUCCESS.name())
                .orderByDesc(KnowledgeIndexJobEntity::getId)
                .last("limit 1"));
    }

    private Long resolveDocumentCreatedBy(Long documentId) {
        KnowledgeDocumentEntity document = documentMapper.selectById(documentId);
        return document == null || document.getCreatedBy() == null ? 0L : document.getCreatedBy();
    }

    private KnowledgeGraphBuildChunkRequest toGraphChunk(KnowledgeChunkEntity chunk) {
        return new KnowledgeGraphBuildChunkRequest(
                chunk.getId(),
                chunk.getChunkIndex(),
                chunk.getContent(),
                chunk.getTitlePath(),
                chunk.getPageNumber(),
                chunk.getSheetName(),
                chunk.getSlideNumber()
        );
    }

    /**
     * 解析知识库图谱领域，未配置时默认 AUTO。
     *
     * @param knowledgeBaseId 知识库 ID
     * @return 传给 AI 服务的 graphDomain
     */
    private String resolveGraphDomain(Long knowledgeBaseId) {
        KnowledgeBaseEntity knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null || knowledgeBase.getGraphDomain() == null || knowledgeBase.getGraphDomain().isBlank()) {
            return "AUTO";
        }
        return knowledgeBase.getGraphDomain().trim().toUpperCase(Locale.ROOT);
    }

    private String toGraphJobStatus(String status, String stage) {
        if (KnowledgeIndexJobStatus.FAILED.name().equals(status) || STATUS_FAILED.equals(status)) {
            return STATUS_FAILED;
        }
        if (KnowledgeIndexJobStatus.SUCCESS.name().equals(status)
                || "INDEXED".equals(status)
                || KnowledgeIndexStage.DONE.name().equals(stage)) {
            return STATUS_SUCCESS;
        }
        return STATUS_RUNNING;
    }

    private String entityKey(String type, String normalizedName) {
        return defaultString(type, "OTHER") + "::" + normalizeName(normalizedName);
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String normalized = String.join(" ", name.trim().toLowerCase(Locale.ROOT).split("\\s+"));
        return containsCjk(normalized) ? normalized.replace(" ", "") : normalized;
    }

    private boolean containsCjk(String value) {
        return value.codePoints().anyMatch(codePoint ->
                (codePoint >= 0x3400 && codePoint <= 0x4DBF)
                        || (codePoint >= 0x4E00 && codePoint <= 0x9FFF)
                        || (codePoint >= 0xF900 && codePoint <= 0xFAFF));
    }

    private BigDecimal defaultConfidence(BigDecimal value) {
        return value == null ? BigDecimal.ONE : value;
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    /**
     * 将空白字符串转为 null，避免可选展示字段落库为空串。
     *
     * @param value 原始字符串
     * @return 非空白字符串或 null
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
