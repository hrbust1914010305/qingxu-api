package com.qingxu.qingxuapi.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(KnowledgeAiProperties.class)
public class DefaultKnowledgeAiClient implements KnowledgeAiClient {

    private final KnowledgeAiProperties properties;

    @Override
    public void ensureAvailable() {
        ensureConfigured();
        String target = properties.getBaseUrl() + "/health";
        log.info("[knowledge-ai] health check start: enabled={}, baseUrl={}, target={}",
                properties.isEnabled(), properties.getBaseUrl(), target);
        try {
            ResponseEntity<Void> response = restClient()
                    .get()
                    .uri("/health")
                    .retrieve()
                    .toBodilessEntity();
            log.info("[knowledge-ai] health check success: target={}, status={}",
                    target, response.getStatusCode());
        } catch (RestClientException exception) {
            log.error("[knowledge-ai] health check failed: target={}, message={}",
                    target, exception.getMessage(), exception);
            throw exception;
        }
    }

    @Override
    public void startIndexing(KnowledgeIndexStartRequest request) {
        startIndexingAccepted(request);
    }

    @Override
    public KnowledgeIndexAcceptedResponse startIndexingAccepted(KnowledgeIndexStartRequest request) {
        ensureConfigured();
        String target = properties.getBaseUrl() + properties.getStartPath();
        log.info("[knowledge-ai] POST rag index start: enabled={}, baseUrl={}, startPath={}, target={}, jobId={}, documentId={}, fileId={}, knowledgeBaseId={}, storagePath={}, originalName={}, extension={}, createdBy={}",
                properties.isEnabled(), properties.getBaseUrl(), properties.getStartPath(), target,
                request.jobId(), request.documentId(), request.fileId(), request.knowledgeBaseId(),
                request.storagePath(), request.originalName(), request.extension(), request.createdBy());
        try {
            ResponseEntity<KnowledgeIndexAcceptedResponse> response = restClient()
                    .post()
                    .uri(properties.getStartPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toEntity(KnowledgeIndexAcceptedResponse.class);
            KnowledgeIndexAcceptedResponse body = response.getBody();
            if (body == null || !Boolean.TRUE.equals(body.accepted())) {
                log.error("[knowledge-ai] POST rag index not accepted: jobId={}, target={}, status={}, body={}",
                        request.jobId(), target, response.getStatusCode(), body);
                throw new IllegalStateException("Knowledge AI index job was not accepted");
            }
            log.info("[knowledge-ai] POST rag index accepted: jobId={}, target={}, status={}, aiJobId={}, message={}",
                    request.jobId(), target, response.getStatusCode(), body.jobId(), body.message());
            return body;
        } catch (RestClientException exception) {
            log.error("[knowledge-ai] POST rag index failed: jobId={}, target={}, message={}",
                    request.jobId(), target, exception.getMessage(), exception);
            throw exception;
        }
    }

    @Override
    public KnowledgeGraphBuildAcceptedResponse startGraphBuild(KnowledgeGraphBuildStartRequest request) {
        ensureConfigured();
        String target = properties.getBaseUrl() + properties.getGraphStartPath();
        log.info("[knowledge-ai] POST graph build start: baseUrl={}, graphStartPath={}, target={}, graphJobId={}, indexJobId={}, documentId={}, knowledgeBaseId={}, chunkCount={}",
                properties.getBaseUrl(), properties.getGraphStartPath(), target,
                request.graphJobId(), request.indexJobId(), request.documentId(), request.knowledgeBaseId(),
                request.chunks() == null ? 0 : request.chunks().size());
        try {
            ResponseEntity<KnowledgeGraphBuildAcceptedResponse> response = restClient()
                    .post()
                    .uri(properties.getGraphStartPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toEntity(KnowledgeGraphBuildAcceptedResponse.class);
            KnowledgeGraphBuildAcceptedResponse body = response.getBody();
            if (body == null || !Boolean.TRUE.equals(body.accepted())) {
                log.error("[knowledge-ai] POST graph build not accepted: graphJobId={}, target={}, status={}, body={}",
                        request.graphJobId(), target, response.getStatusCode(), body);
                throw new IllegalStateException("Knowledge AI graph build job was not accepted");
            }
            log.info("[knowledge-ai] POST graph build accepted: graphJobId={}, target={}, status={}, aiGraphJobId={}, message={}",
                    request.graphJobId(), target, response.getStatusCode(), body.graphJobId(), body.message());
            return body;
        } catch (RestClientException exception) {
            log.error("[knowledge-ai] POST graph build failed: graphJobId={}, target={}, message={}",
                    request.graphJobId(), target, exception.getMessage(), exception);
            throw exception;
        }
    }

    @Override
    public KnowledgeGraphRuntimeStatusResponse graphRuntimeStatus(Long graphJobId) {
        ensureConfigured();
        String target = properties.getBaseUrl() + properties.getGraphStatusPath().replace("{graphJobId}", String.valueOf(graphJobId));
        log.info("[knowledge-ai] GET graph runtime status start: baseUrl={}, graphStatusPath={}, target={}, graphJobId={}",
                properties.getBaseUrl(), properties.getGraphStatusPath(), target, graphJobId);
        try {
            ResponseEntity<KnowledgeGraphRuntimeStatusResponse> response = restClient()
                    .get()
                    .uri(properties.getGraphStatusPath(), graphJobId)
                    .retrieve()
                    .toEntity(KnowledgeGraphRuntimeStatusResponse.class);
            KnowledgeGraphRuntimeStatusResponse body = response.getBody();
            if (body == null) {
                log.warn("[knowledge-ai] GET graph runtime status empty body: graphJobId={}, target={}, status={}",
                        graphJobId, target, response.getStatusCode());
                return new KnowledgeGraphRuntimeStatusResponse(graphJobId, false, "NOT_RUNNING");
            }
            log.info("[knowledge-ai] GET graph runtime status success: graphJobId={}, target={}, status={}, running={}, aiStatus={}",
                    graphJobId, target, response.getStatusCode(), body.running(), body.status());
            return body;
        } catch (RestClientException exception) {
            log.warn("[knowledge-ai] GET graph runtime status failed: graphJobId={}, target={}, message={}",
                    graphJobId, target, exception.getMessage(), exception);
            return new KnowledgeGraphRuntimeStatusResponse(graphJobId, false, "NOT_RUNNING");
        }
    }

    @Override
    public void cancelIndexing(Long jobId) {
        ensureConfigured();
        String target = properties.getBaseUrl() + properties.getCancelPath();
        log.info("[knowledge-ai] POST rag index cancel start: enabled={}, baseUrl={}, cancelPath={}, target={}, jobId={}",
                properties.isEnabled(), properties.getBaseUrl(), properties.getCancelPath(), target, jobId);
        try {
            ResponseEntity<KnowledgeIndexCancelResponse> response = restClient()
                    .post()
                    .uri(properties.getCancelPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(new KnowledgeIndexCancelRequest(jobId))
                    .retrieve()
                    .toEntity(KnowledgeIndexCancelResponse.class);
            KnowledgeIndexCancelResponse body = response.getBody();
            if (body == null || !Boolean.TRUE.equals(body.canceled())) {
                log.warn("[knowledge-ai] POST rag index cancel not accepted: jobId={}, target={}, status={}, body={}",
                        jobId, target, response.getStatusCode(), body);
                return;
            }
            log.info("[knowledge-ai] POST rag index cancel accepted: jobId={}, target={}, status={}, aiJobId={}, message={}",
                    jobId, target, response.getStatusCode(), body.jobId(), body.message());
        } catch (RestClientException exception) {
            log.warn("[knowledge-ai] POST rag index cancel failed: jobId={}, target={}, message={}",
                    jobId, target, exception.getMessage(), exception);
            throw exception;
        }
    }

    private void ensureConfigured() {
        if (!properties.isEnabled()) {
            log.error("[knowledge-ai] AI service disabled by config: enabled={}, baseUrl={}, startPath={}, cancelPath={}",
                    properties.isEnabled(), properties.getBaseUrl(), properties.getStartPath(), properties.getCancelPath());
            throw new IllegalStateException("Knowledge AI service is disabled");
        }
        if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()) {
            log.error("[knowledge-ai] AI service baseUrl is blank: enabled={}, baseUrl={}, startPath={}, cancelPath={}",
                    properties.isEnabled(), properties.getBaseUrl(), properties.getStartPath(), properties.getCancelPath());
            throw new IllegalStateException("Knowledge AI service baseUrl is blank");
        }
    }

    private RestClient restClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(properties.getBaseUrl())
                .build();
    }
}
