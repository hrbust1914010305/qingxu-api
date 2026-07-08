package com.qingxu.qingxuapi.domain.file;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorageService {

    FileStorageObject store(MultipartFile file, String bizType, String extension) throws IOException;

    String storeChunk(String uploadId, Integer chunkIndex, MultipartFile chunk) throws IOException;

    InputStream load(String storagePath) throws IOException;

    FileStorageObject mergeChunks(String uploadId, int totalChunks, String bizType, String extension) throws IOException;

    void delete(String storagePath) throws IOException;

    void deleteChunks(String uploadId) throws IOException;
}
