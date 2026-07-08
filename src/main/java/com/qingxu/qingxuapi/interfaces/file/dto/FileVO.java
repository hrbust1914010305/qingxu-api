package com.qingxu.qingxuapi.interfaces.file.dto;

public record FileVO(
        Long id,
        String name,
        String url,
        String mimeType,
        Long size,
        String checksum,
        String bizType
) {
}
