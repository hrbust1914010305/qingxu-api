package com.qingxu.qingxuapi.interfaces.knowledge.v2;

import com.qingxu.qingxuapi.application.auth.AuthApplicationService;
import com.qingxu.qingxuapi.application.knowledge.KnowledgeGraphApplicationService;
import com.qingxu.qingxuapi.application.knowledge.KnowledgeGraphBuildApplicationService;
import com.qingxu.qingxuapi.application.knowledge.KnowledgeV2ApplicationService;
import com.qingxu.qingxuapi.common.exception.BusinessException;
import com.qingxu.qingxuapi.common.response.ApiResponse;
import com.qingxu.qingxuapi.common.response.ErrorCode;
import com.qingxu.qingxuapi.common.response.PageResponse;
import com.qingxu.qingxuapi.common.response.ResponseFactory;
import com.qingxu.qingxuapi.infrastructure.ai.KnowledgeAiProperties;
import com.qingxu.qingxuapi.interfaces.auth.dto.CurrentUserResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.dto.KnowledgeChunkResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.dto.KnowledgeGraphEntityDetailResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.dto.KnowledgeGraphRelationDetailResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2CreateRequest;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeGraphProgressCallbackRequest;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2CreateResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2GraphRegenerateRequest;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2GraphResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2ListItemResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2ParseRequest;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2ParseResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2ProgressCallbackRequest;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2ProgressResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2RemoveResponse;
import com.qingxu.qingxuapi.interfaces.knowledge.v2.dto.KnowledgeV2StopParseResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class KnowledgeV2Controller {

    private final KnowledgeV2ApplicationService knowledgeService;
    private final KnowledgeGraphApplicationService graphService;
    private final KnowledgeGraphBuildApplicationService graphBuildService;
    private final AuthApplicationService authApplicationService;
    private final ResponseFactory responseFactory;
    private final KnowledgeAiProperties knowledgeAiProperties;

    @PostMapping("/create")
    public ApiResponse<KnowledgeV2CreateResponse> create(@Valid @RequestBody KnowledgeV2CreateRequest request,
                                                         HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        return responseFactory.success(knowledgeService.create(request, currentUser));
    }

    @GetMapping("/list")
    public ApiResponse<PageResponse<KnowledgeV2ListItemResponse>> list(@RequestParam(required = false) String keyword,
                                                                       @RequestParam(required = false) String visibility,
                                                                       @RequestParam(required = false) String status,
                                                                       @RequestParam(defaultValue = "1") long page,
                                                                       @RequestParam(defaultValue = "10") long pageSize,
                                                                       HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        return responseFactory.success(knowledgeService.list(keyword, visibility, status, page, pageSize, currentUser));
    }

    @DeleteMapping("/remove/{knowledgeId}")
    public ApiResponse<KnowledgeV2RemoveResponse> remove(@PathVariable Long knowledgeId,
                                                         HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        return responseFactory.success(knowledgeService.remove(knowledgeId, currentUser));
    }

    @PostMapping("/parse")
    public ApiResponse<KnowledgeV2ParseResponse> parse(@Valid @RequestBody KnowledgeV2ParseRequest request,
                                                       HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        return responseFactory.success(knowledgeService.parse(request, currentUser));
    }

    @PostMapping("/reparse/{documentId}")
    public ApiResponse<KnowledgeV2ParseResponse> reparse(@PathVariable Long documentId,
                                                         HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        return responseFactory.success(knowledgeService.reparseDocument(documentId, currentUser));
    }

    @PostMapping("/parse/stop")
    public ApiResponse<KnowledgeV2StopParseResponse> stopParse(@Valid @RequestBody KnowledgeV2ParseRequest request,
                                                               HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        return responseFactory.success(knowledgeService.stopParse(request, currentUser));
    }

    @GetMapping("/progress/{knowledgeId}")
    public ApiResponse<KnowledgeV2ProgressResponse> progress(@PathVariable Long knowledgeId,
                                                             HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        return responseFactory.success(knowledgeService.progress(knowledgeId, currentUser));
    }

    @GetMapping("/chunks/{knowledgeId}")
    public ApiResponse<PageResponse<KnowledgeChunkResponse>> chunks(@PathVariable Long knowledgeId,
                                                                    @RequestParam(required = false) Long documentId,
                                                                    @RequestParam(required = false) String keyword,
                                                                    @RequestParam(required = false) String contentType,
                                                                    @RequestParam(defaultValue = "1") long page,
                                                                    @RequestParam(defaultValue = "20") long pageSize,
                                                                    HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        return responseFactory.success(knowledgeService.chunks(knowledgeId, documentId, page, pageSize, keyword, contentType, currentUser));
    }

    @GetMapping("/graph/{knowledgeId}")
    public ApiResponse<KnowledgeV2GraphResponse> graph(@PathVariable Long knowledgeId,
                                                       @RequestParam(required = false) List<String> entityTypes,
                                                       @RequestParam(required = false) List<String> relationTypes,
                                                       @RequestParam(required = false) Long fileId,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Integer depth,
                                                       @RequestParam(required = false) Integer limit,
                                                       HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        return responseFactory.success(graphService.graphWithStatus(knowledgeId, entityTypes, relationTypes, fileId, keyword, depth, limit, currentUser));
    }

    @PostMapping("/graph/regenerate")
    public ApiResponse<KnowledgeV2GraphResponse> regenerateGraph(@Valid @RequestBody KnowledgeV2GraphRegenerateRequest request,
                                                                 HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        return responseFactory.success(graphService.regenerateGraph(request.knowledgeId(), request.fileId(), currentUser));
    }

    @GetMapping("/graph-entity/{entityId}")
    public ApiResponse<KnowledgeGraphEntityDetailResponse> entityDetail(@PathVariable Long entityId,
                                                                        @RequestParam Long knowledgeId,
                                                                        HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        KnowledgeGraphEntityDetailResponse response = graphService.entityDetail(entityId, currentUser);
        if (!knowledgeId.equals(response.knowledgeBaseId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return responseFactory.success(response);
    }

    @GetMapping("/graph-relation/{relationId}")
    public ApiResponse<KnowledgeGraphRelationDetailResponse> relationDetail(@PathVariable Long relationId,
                                                                            @RequestParam Long knowledgeId,
                                                                            HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        KnowledgeGraphRelationDetailResponse response = graphService.relationDetail(relationId, currentUser);
        if (!knowledgeId.equals(response.knowledgeBaseId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return responseFactory.success(response);
    }

    @PostMapping("/progress/report")
    public ApiResponse<Void> reportProgress(@Valid @RequestBody KnowledgeV2ProgressCallbackRequest request,
                                            HttpServletRequest servletRequest) {
        validateAiCallbackToken(servletRequest);
        knowledgeService.reportProgress(request);
        return responseFactory.success();
    }

    @PostMapping("/graph-progress/report")
    public ApiResponse<Void> reportGraphProgress(@Valid @RequestBody KnowledgeGraphProgressCallbackRequest request,
                                                 HttpServletRequest servletRequest) {
        validateAiCallbackToken(servletRequest);
        graphBuildService.reportProgress(request);
        return responseFactory.success();
    }

    private void validateAiCallbackToken(HttpServletRequest servletRequest) {
        String expectedToken = knowledgeAiProperties.getCallbackToken();
        if (expectedToken == null || expectedToken.isBlank()) {
            return;
        }
        String actualToken = servletRequest.getHeader("X-Qingxu-Ai-Token");
        if (!expectedToken.equals(actualToken)) {
            throw new BusinessException(ErrorCode.AUTH_403);
        }
    }
}

