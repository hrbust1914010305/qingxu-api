package com.qingxu.qingxuapi.interfaces.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingxu.qingxuapi.infrastructure.captcha.CaptchaChallenge;
import com.qingxu.qingxuapi.infrastructure.captcha.CaptchaService;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Test
    void uploadRequiresLogin() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes(StandardCharsets.UTF_8)
        );

        String response = mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertThat(root.get("code").asText()).isEqualTo("AUTH_401");
    }

    @Test
    void uploadStoresFileAndDownloadStreamsContent() throws Exception {
        Cookie cookie = loginActiveUser("file-user");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "hello file".getBytes(StandardCharsets.UTF_8)
        );

        String uploadResponse = mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .param("bizType", "knowledge")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode uploadRoot = objectMapper.readTree(uploadResponse);
        assertThat(uploadRoot.get("code").asText()).isEqualTo("0");
        assertThat(uploadRoot.at("/data/id").asLong()).isPositive();
        assertThat(uploadRoot.at("/data/name").asText()).isEqualTo("hello.txt");
        assertThat(uploadRoot.at("/data/url").asText()).startsWith("/api/files/");
        assertThat(uploadRoot.at("/data/size").asLong()).isEqualTo(10);
        assertThat(uploadRoot.at("/data/bizType").asText()).isEqualTo("knowledge");

        long fileId = uploadRoot.at("/data/id").asLong();
        byte[] downloadBytes = mockMvc.perform(get("/api/files/{id}/download", fileId).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.TEXT_PLAIN_VALUE))
                .andExpect(header().longValue("Content-Length", 10))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(new String(downloadBytes, StandardCharsets.UTF_8)).isEqualTo("hello file");
    }

    @Test
    void uploadRejectsDisallowedExtension() throws Exception {
        Cookie cookie = loginActiveUser("extension-user");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malware.exe",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "bad".getBytes(StandardCharsets.UTF_8)
        );

        String response = mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .cookie(cookie))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertThat(root.get("code").asText()).isEqualTo("FILE_TYPE_NOT_ALLOWED");
    }

    @Test
    void multipartUploadCanResumeCompleteAndDownload() throws Exception {
        Cookie cookie = loginActiveUser("multipart-user");
        Map<String, Object> initRequest = Map.of(
                "fileName", "large.txt",
                "mimeType", MediaType.TEXT_PLAIN_VALUE,
                "size", 11,
                "chunkSize", 5,
                "fingerprint", "large.txt_11_test",
                "bizType", "knowledge"
        );

        String initResponse = mockMvc.perform(post("/api/files/multipart/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initRequest))
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode initRoot = objectMapper.readTree(initResponse);
        String uploadId = initRoot.at("/data/uploadId").asText();
        assertThat(uploadId).isNotBlank();
        assertThat(initRoot.at("/data/totalChunks").asInt()).isEqualTo(3);
        assertThat(initRoot.at("/data/uploadedChunks").isArray()).isTrue();

        uploadChunk(cookie, uploadId, 0, "hello");
        uploadChunk(cookie, uploadId, 1, " worl");

        String statusResponse = mockMvc.perform(get("/api/files/multipart/{uploadId}/status", uploadId).cookie(cookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode statusRoot = objectMapper.readTree(statusResponse);
        assertThat(statusRoot.at("/data/uploadedChunks")).hasSize(2);

        uploadChunk(cookie, uploadId, 2, "d");

        String completeResponse = mockMvc.perform(post("/api/files/multipart/{uploadId}/complete", uploadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode completeRoot = objectMapper.readTree(completeResponse);
        long fileId = completeRoot.at("/data/id").asLong();
        byte[] downloadBytes = mockMvc.perform(get("/api/files/{id}/download", fileId).cookie(cookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(new String(downloadBytes, StandardCharsets.UTF_8)).isEqualTo("hello world");
    }

    @Test
    void deletePreventsFutureDownload() throws Exception {
        Cookie cookie = loginActiveUser("delete-user");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "delete.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "delete me".getBytes(StandardCharsets.UTF_8)
        );
        String uploadResponse = mockMvc.perform(multipart("/api/files/upload").file(file).cookie(cookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long fileId = objectMapper.readTree(uploadResponse).at("/data/id").asLong();

        mockMvc.perform(delete("/api/files/{id}", fileId).cookie(cookie))
                .andExpect(status().isOk());

        String downloadResponse = mockMvc.perform(get("/api/files/{id}/download", fileId).cookie(cookie))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(downloadResponse);
        assertThat(root.get("code").asText()).isEqualTo("FILE_NOT_FOUND");
    }

    private void uploadChunk(Cookie cookie, String uploadId, int chunkIndex, String content) throws Exception {
        MockMultipartFile chunk = new MockMultipartFile(
                "chunk",
                "chunk-%d.part".formatted(chunkIndex),
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                content.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/files/multipart/{uploadId}/chunks/{chunkIndex}", uploadId, chunkIndex)
                        .file(chunk)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .cookie(cookie))
                .andExpect(status().isOk());
    }

    private Cookie loginActiveUser(String username) throws Exception {
        registerUser(username);
        SysUserEntity user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, username));
        user.setStatus("ACTIVE");
        sysUserMapper.updateById(user);

        CaptchaChallenge loginCaptcha = captchaService.createCaptcha();
        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", "Password123!",
                                "captchaKey", loginCaptcha.captchaKey(),
                                "captcha", loginCaptcha.answer()
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = loginResult.getResponse().getCookie("QINGXU_SESSION");
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private void registerUser(String username) throws Exception {
        CaptchaChallenge captcha = captchaService.createCaptcha();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "nickname", "File User",
                                "email", username + "@example.com",
                                "phone", "13900000000",
                                "password", "Password123!",
                                "confirmPassword", "Password123!",
                                "captchaKey", captcha.captchaKey(),
                                "captcha", captcha.answer(),
                                "agreePolicy", true
                        ))))
                .andExpect(status().isOk());
    }
}
