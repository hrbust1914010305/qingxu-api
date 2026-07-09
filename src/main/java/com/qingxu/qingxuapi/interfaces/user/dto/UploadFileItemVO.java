package com.qingxu.qingxuapi.interfaces.user.dto;

public record UploadFileItemVO(
        Long id,
        String uid,
        String name,
        String url,
        Long size,
        String status,
        Integer percent
) {
}
