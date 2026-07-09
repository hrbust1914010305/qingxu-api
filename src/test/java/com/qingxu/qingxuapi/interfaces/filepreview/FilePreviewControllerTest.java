package com.qingxu.qingxuapi.interfaces.filepreview;

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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
class FilePreviewControllerTest {

    private static final Pattern SOURCE_TOKEN_PATTERN = Pattern.compile("/api/file-preview/source/([^?&]+)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Test
    void previewUrlCanBeGeneratedByUserWhoDidNotCreateFile() throws Exception {
        Cookie ownerCookie = loginActiveUser("preview-owner");
        long fileId = uploadTextFile(ownerCookie, "shared.txt", "shared preview");
        Cookie readerCookie = loginActiveUser("preview-reader");

        String response = mockMvc.perform(get("/api/file-preview/{fileId}/url", fileId).cookie(readerCookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertThat(root.get("code").asText()).isEqualTo("0");
        assertThat(root.at("/data/fileId").asLong()).isEqualTo(fileId);
        assertThat(root.at("/data/fileName").asText()).isEqualTo("shared.txt");
        assertThat(root.at("/data/status").asText()).isEqualTo("READY");
        assertThat(root.at("/data/previewUrl").asText())
                .startsWith("http://127.0.0.1:8012/onlinePreview")
                .contains("url=");
        assertThat(decodePreviewSourceUrl(root.at("/data/previewUrl").asText()))
                .startsWith("http://127.0.0.1:8081/api/file-preview/source/")
                .contains("fullfilename=shared");
    }

    @Test
    void previewSourceStreamsFileWithoutLoginWhenTokenIsValid() throws Exception {
        Cookie ownerCookie = loginActiveUser("preview-source-owner");
        long fileId = uploadTextFile(ownerCookie, "source.txt", "source preview");
        String previewResponse = mockMvc.perform(get("/api/file-preview/{fileId}/url", fileId).cookie(ownerCookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = extractSourceToken(objectMapper.readTree(previewResponse).at("/data/previewUrl").asText());

        byte[] bytes = mockMvc.perform(get("/api/file-preview/source/{token}", token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.TEXT_PLAIN_VALUE))
                .andExpect(header().longValue("Content-Length", 14))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("source preview");
    }

    @Test
    void previewDoesNotRelaxDeleteOwnership() throws Exception {
        Cookie ownerCookie = loginActiveUser("preview-delete-owner");
        long fileId = uploadTextFile(ownerCookie, "owned.txt", "owned content");
        Cookie readerCookie = loginActiveUser("preview-delete-reader");

        mockMvc.perform(get("/api/file-preview/{fileId}/url", fileId).cookie(readerCookie))
                .andExpect(status().isOk());

        String deleteResponse = mockMvc.perform(delete("/api/files/{id}", fileId).cookie(readerCookie))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(deleteResponse);
        assertThat(root.get("code").asText()).isEqualTo("FILE_NOT_FOUND");
    }

    private long uploadTextFile(Cookie cookie, String fileName, String content) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                MediaType.TEXT_PLAIN_VALUE,
                content.getBytes(StandardCharsets.UTF_8)
        );
        String uploadResponse = mockMvc.perform(multipart("/api/files/upload").file(file).cookie(cookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(uploadResponse).at("/data/id").asLong();
    }

    private String extractSourceToken(String previewUrl) {
        Matcher matcher = SOURCE_TOKEN_PATTERN.matcher(decodePreviewSourceUrl(previewUrl));
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String decodePreviewSourceUrl(String previewUrl) {
        String decodedPreviewUrl = URLDecoder.decode(previewUrl, StandardCharsets.UTF_8);
        String encodedSourceUrl = decodedPreviewUrl.substring(decodedPreviewUrl.indexOf("url=") + 4);
        return new String(Base64.getDecoder().decode(encodedSourceUrl), StandardCharsets.UTF_8);
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
                                "nickname", "File Preview User",
                                "email", username + "@example.com",
                                "phone", phoneFor(username),
                                "password", "Password123!",
                                "confirmPassword", "Password123!",
                                "captchaKey", captcha.captchaKey(),
                                "captcha", captcha.answer(),
                                "agreePolicy", true
                        ))))
                .andExpect(status().isOk());
    }

    private String phoneFor(String username) {
        int suffix = Math.abs(username.hashCode() % 100_000_000);
        return "139%08d".formatted(suffix);
    }
}
