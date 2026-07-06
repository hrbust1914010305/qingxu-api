package com.qingxu.qingxuapi.interfaces.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingxu.qingxuapi.application.auth.AuthApplicationService;
import com.qingxu.qingxuapi.infrastructure.captcha.CaptchaChallenge;
import com.qingxu.qingxuapi.infrastructure.captcha.CaptchaService;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Test
    void captchaReturnsUnifiedResponseWithoutAnswer() throws Exception {
        String response = mockMvc.perform(post("/api/auth/captcha"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertThat(root.get("code").asText()).isEqualTo("0");
        assertThat(root.get("message").asText()).isEqualTo("操作成功");
        assertThat(root.get("traceId").asText()).isNotBlank();
        assertThat(root.get("timestamp").asText()).isNotBlank();
        assertThat(root.at("/data/captchaKey").asText()).isNotBlank();
        assertThat(root.at("/data/imageBase64").asText()).startsWith("data:image/png;base64,");
        assertThat(root.at("/data/expiresIn").asInt()).isEqualTo(120);
        assertThat(root.at("/data/answer").isMissingNode()).isTrue();
    }

    @Test
    void currentUserRequiresLogin() throws Exception {
        String response = mockMvc.perform(get("/api/auth/current-user"))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertThat(root.get("code").asText()).isEqualTo("AUTH_401");
        assertThat(root.get("traceId").asText()).isNotBlank();
    }

    @Test
    void protectedApiRequiresLogin() throws Exception {
        String response = mockMvc.perform(get("/api/protected/ping"))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertThat(root.get("code").asText()).isEqualTo("AUTH_401");
    }

    @Test
    void registerRejectsMissingRequiredFields() throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertThat(root.get("code").asText()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void loginFailureUsesUnifiedCredentialError() throws Exception {
        CaptchaChallenge captcha = captchaService.createCaptcha();
        Map<String, Object> request = Map.of(
                "username", "missing-user",
                "password", "WrongPassword1!",
                "captchaKey", captcha.captchaKey(),
                "captcha", captcha.answer()
        );

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertThat(root.get("code").asText()).isEqualTo("AUTH_BAD_CREDENTIALS");
    }

    @Test
    void registerReturnsPendingStatusAndMessage() throws Exception {
        CaptchaChallenge captcha = captchaService.createCaptcha();
        Map<String, Object> request = Map.of(
                "username", "zhangsan",
                "nickname", "三木",
                "email", "zhangsan@example.com",
                "phone", "13800000000",
                "password", "Password123!",
                "confirmPassword", "Password123!",
                "captchaKey", captcha.captchaKey(),
                "captcha", captcha.answer(),
                "agreePolicy", true
        );

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertThat(root.get("code").asText()).isEqualTo("0");
        assertThat(root.at("/data/registered").asBoolean()).isTrue();
        assertThat(root.at("/data/status").asText()).isEqualTo("PENDING");
        assertThat(root.at("/data/message").asText()).contains("审核");

        SysUserEntity user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, "zhangsan"));
        assertThat(user).isNotNull();
        assertThat(user.getPasswordHash()).isNotEqualTo("Password123!");
        assertThat(user.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void pendingUserCannotLoginAndStillGetsUnifiedCredentialError() throws Exception {
        registerUser("pending-user");

        CaptchaChallenge loginCaptcha = captchaService.createCaptcha();
        Map<String, Object> loginRequest = Map.of(
                "username", "pending-user",
                "password", "Password123!",
                "captchaKey", loginCaptcha.captchaKey(),
                "captcha", loginCaptcha.answer()
        );

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertThat(root.get("code").asText()).isEqualTo("AUTH_BAD_CREDENTIALS");
    }

    @Test
    void loginFailureIncrementsFailedCountAndLocksAfterFiveAttempts() throws Exception {
        registerUser("lock-user");
        SysUserEntity user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, "lock-user"));
        user.setStatus("ACTIVE");
        sysUserMapper.updateById(user);

        for (int i = 0; i < 5; i++) {
            CaptchaChallenge loginCaptcha = captchaService.createCaptcha();
            Map<String, Object> loginRequest = Map.of(
                    "username", "lock-user",
                    "password", "WrongPassword123",
                    "captchaKey", loginCaptcha.captchaKey(),
                    "captcha", loginCaptcha.answer()
            );

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest());
        }

        SysUserEntity lockedUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, "lock-user"));
        assertThat(lockedUser.getFailedLoginCount()).isEqualTo(5);
        assertThat(lockedUser.getLockedUntil()).isNotNull();
    }

    @Test
    void loginAndCurrentUserWorkAfterDatabaseActivation() throws Exception {
        registerUser("active-user");
        SysUserEntity user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, "active-user"));
        user.setStatus("ACTIVE");
        sysUserMapper.updateById(user);

        CaptchaChallenge loginCaptcha = captchaService.createCaptcha();
        Map<String, Object> loginRequest = Map.of(
                "username", "active-user",
                "password", "Password123!",
                "captchaKey", loginCaptcha.captchaKey(),
                "captcha", loginCaptcha.answer()
        );

        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        JsonNode loginRoot = objectMapper.readTree(loginResponse);
        assertThat(loginRoot.at("/data/username").asText()).isEqualTo("active-user");
        assertThat(loginRoot.at("/data/status").asText()).isEqualTo("ACTIVE");

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute(AuthApplicationService.SESSION_CURRENT_USER)).isNotNull();
        assertThat(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNotNull();

        String currentUserResponse = mockMvc.perform(get("/api/auth/current-user").session(session))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode currentUserRoot = objectMapper.readTree(currentUserResponse);
        assertThat(currentUserRoot.at("/data/username").asText()).isEqualTo("active-user");
        assertThat(currentUserRoot.at("/data/tenantId").asText()).isEqualTo("default");
        assertThat(currentUserRoot.at("/data/roles").isArray()).isTrue();
        assertThat(currentUserRoot.at("/data/permissions").isArray()).isTrue();

        String protectedResponse = mockMvc.perform(get("/api/protected/ping").session(session))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode protectedRoot = objectMapper.readTree(protectedResponse);
        assertThat(protectedRoot.at("/data").asText()).isEqualTo("pong");
    }

    @Test
    void routesAndPermissionsReturnEmptyArrays() throws Exception {
        registerUser("perm-test-user");
        SysUserEntity user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, "perm-test-user"));
        user.setStatus("ACTIVE");
        sysUserMapper.updateById(user);

        CaptchaChallenge loginCaptcha = captchaService.createCaptcha();
        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "perm-test-user",
                                "password", "Password123!",
                                "captchaKey", loginCaptcha.captchaKey(),
                                "captcha", loginCaptcha.answer()
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        String routesResponse = mockMvc.perform(get("/api/auth/routes").session(session))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode routesRoot = objectMapper.readTree(routesResponse);
        assertThat(routesRoot.at("/data").isArray()).isTrue();

        String permissionsResponse = mockMvc.perform(get("/api/auth/permissions/current").session(session))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode permissionsRoot = objectMapper.readTree(permissionsResponse);
        assertThat(permissionsRoot.at("/data/roles").isArray()).isTrue();
        assertThat(permissionsRoot.at("/data/permissions").isArray()).isTrue();
    }

    private void registerUser(String username) throws Exception {
        CaptchaChallenge captcha = captchaService.createCaptcha();
        Map<String, Object> request = Map.of(
                "username", username,
                "nickname", "Pending",
                "email", username + "@example.com",
                "phone", "13900000000",
                "password", "Password123!",
                "confirmPassword", "Password123!",
                "captchaKey", captcha.captchaKey(),
                "captcha", captcha.answer(),
                "agreePolicy", true
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void registerRejectsWeakPassword() throws Exception {
        CaptchaChallenge captcha = captchaService.createCaptcha();

        Map<String, Object> requestWithShortPassword = Map.of(
                "username", "weak-user",
                "nickname", "Weak",
                "email", "weak@example.com",
                "phone", "13800000001",
                "password", "weak",
                "confirmPassword", "weak",
                "captchaKey", captcha.captchaKey(),
                "captcha", captcha.answer(),
                "agreePolicy", true
        );

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithShortPassword)))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertThat(root.get("code").asText()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void lockedUserCannotLoginAndReceivesLockedError() throws Exception {
        registerUser("locked-test-user");
        SysUserEntity user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, "locked-test-user"));
        user.setStatus("ACTIVE");
        user.setFailedLoginCount(5);
        user.setLockedUntil(java.time.LocalDateTime.now().plusMinutes(10));
        sysUserMapper.updateById(user);

        CaptchaChallenge loginCaptcha = captchaService.createCaptcha();
        Map<String, Object> loginRequest = Map.of(
                "username", "locked-test-user",
                "password", "Password123!",
                "captchaKey", loginCaptcha.captchaKey(),
                "captcha", loginCaptcha.answer()
        );

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isLocked())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertThat(root.get("code").asText()).isEqualTo("AUTH_ACCOUNT_LOCKED");
    }
}
