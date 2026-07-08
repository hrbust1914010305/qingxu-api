package com.qingxu.qingxuapi.application.file;

import java.io.InputStream;

public record FileDownloadResource(
        String originalName,
        String mimeType,
        long size,
        InputStream inputStream
) {
}
