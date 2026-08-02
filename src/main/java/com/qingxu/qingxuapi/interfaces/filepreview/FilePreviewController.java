package com.qingxu.qingxuapi.interfaces.filepreview;

import com.qingxu.qingxuapi.application.auth.AuthApplicationService;
import com.qingxu.qingxuapi.application.file.FileDownloadResource;
import com.qingxu.qingxuapi.application.filepreview.FilePreviewApplicationService;
import com.qingxu.qingxuapi.common.response.ApiResponse;
import com.qingxu.qingxuapi.common.response.ResponseFactory;
import com.qingxu.qingxuapi.interfaces.filepreview.dto.FilePreviewStatusResponse;
import com.qingxu.qingxuapi.interfaces.filepreview.dto.FilePreviewUrlResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/file-preview")
@RequiredArgsConstructor
public class FilePreviewController {

    private final FilePreviewApplicationService filePreviewApplicationService;
    private final AuthApplicationService authApplicationService;
    private final ResponseFactory responseFactory;

    @GetMapping("/{fileId}/url")
    public ApiResponse<FilePreviewUrlResponse> url(@PathVariable Long fileId, HttpServletRequest request) {
        authApplicationService.currentUser(request);
        return responseFactory.success(filePreviewApplicationService.generatePreviewUrl(fileId));
    }

    @GetMapping("/{fileId}/status")
    public ApiResponse<FilePreviewStatusResponse> status(@PathVariable Long fileId, HttpServletRequest request) {
        authApplicationService.currentUser(request);
        return responseFactory.success(filePreviewApplicationService.getStatus(fileId));
    }

    @GetMapping("/source/{token}")
    public void source(@PathVariable String token, HttpServletResponse response) throws IOException {
        FileDownloadResource resource = filePreviewApplicationService.getSource(token);
        response.setContentType(resource.mimeType());
        response.setContentLengthLong(resource.size());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(resource.originalName()));
        try (var input = resource.inputStream()) {
            StreamUtils.copy(input, response.getOutputStream());
        }
    }

    private String contentDisposition(String originalName) {
        String encoded = URLEncoder.encode(originalName, StandardCharsets.UTF_8).replace("+", "%20");
        return "inline; filename*=UTF-8''" + encoded;
    }
}
