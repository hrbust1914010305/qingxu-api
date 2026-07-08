package com.qingxu.qingxuapi.interfaces.file;

import com.qingxu.qingxuapi.application.auth.AuthApplicationService;
import com.qingxu.qingxuapi.application.file.FileApplicationService;
import com.qingxu.qingxuapi.application.file.FileDownloadResource;
import com.qingxu.qingxuapi.common.config.QingxuFileProperties;
import com.qingxu.qingxuapi.common.response.ApiResponse;
import com.qingxu.qingxuapi.common.response.ResponseFactory;
import com.qingxu.qingxuapi.interfaces.auth.dto.CurrentUserResponse;
import com.qingxu.qingxuapi.interfaces.file.dto.FileChunkUploadResponse;
import com.qingxu.qingxuapi.interfaces.file.dto.FileUploadCompleteRequest;
import com.qingxu.qingxuapi.interfaces.file.dto.FileUploadInitRequest;
import com.qingxu.qingxuapi.interfaces.file.dto.FileUploadInitResponse;
import com.qingxu.qingxuapi.interfaces.file.dto.FileUploadStatusResponse;
import com.qingxu.qingxuapi.interfaces.file.dto.FileVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileApplicationService fileApplicationService;
    private final AuthApplicationService authApplicationService;
    private final ResponseFactory responseFactory;
    private final QingxuFileProperties fileProperties;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileVO> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "bizType", required = false) String bizType,
                                      HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        return responseFactory.success(fileApplicationService.upload(file, bizType, currentUser));
    }

    @PostMapping("/multipart/init")
    public ApiResponse<FileUploadInitResponse> initMultipart(@Valid @RequestBody FileUploadInitRequest request,
                                                             HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        return responseFactory.success(fileApplicationService.initMultipart(request, currentUser));
    }

    @GetMapping("/multipart/{uploadId}/status")
    public ApiResponse<FileUploadStatusResponse> getMultipartStatus(@PathVariable String uploadId,
                                                                    HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        return responseFactory.success(fileApplicationService.getMultipartStatus(uploadId, currentUser));
    }

    @PutMapping(value = "/multipart/{uploadId}/chunks/{chunkIndex}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileChunkUploadResponse> uploadChunk(@PathVariable String uploadId,
                                                            @PathVariable Integer chunkIndex,
                                                            @RequestParam("chunk") MultipartFile chunk,
                                                            @RequestParam(value = "checksum", required = false) String checksum,
                                                            HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        fileApplicationService.uploadChunk(uploadId, chunkIndex, chunk, checksum, currentUser);
        return responseFactory.success(new FileChunkUploadResponse(chunkIndex, true));
    }

    @PostMapping("/multipart/{uploadId}/complete")
    public ApiResponse<FileVO> completeMultipart(@PathVariable String uploadId,
                                                 @RequestBody(required = false) FileUploadCompleteRequest request,
                                                 HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        return responseFactory.success(fileApplicationService.completeMultipart(uploadId, request, currentUser));
    }

    @DeleteMapping("/multipart/{uploadId}")
    public ApiResponse<Void> cancelMultipart(@PathVariable String uploadId, HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        fileApplicationService.cancelMultipart(uploadId, currentUser);
        return responseFactory.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteFile(@PathVariable Long id, HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        fileApplicationService.deleteFile(id, currentUser);
        return responseFactory.success();
    }

    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, HttpServletRequest servletRequest, HttpServletResponse response) throws IOException {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        FileDownloadResource resource = fileApplicationService.getDownloadResource(id, currentUser);
        response.setContentType(resource.mimeType());
        response.setContentLengthLong(resource.size());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(resource.originalName()));
        try (var input = resource.inputStream()) {
            StreamUtils.copy(input, response.getOutputStream());
        }
    }

    private String contentDisposition(String originalName) {
        String encoded = URLEncoder.encode(originalName, StandardCharsets.UTF_8).replace("+", "%20");
        return fileProperties.getDownloadDisposition() + "; filename*=UTF-8''" + encoded;
    }
}
