package com.qingxu.qingxuapi.interfaces.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingxu.qingxuapi.infrastructure.captcha.CaptchaChallenge;
import com.qingxu.qingxuapi.infrastructure.captcha.CaptchaService;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysRoleEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserRoleMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerAssignRolesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    /**
     * Helper: 创建一个普通用户（不需要密码可登录，仅用于关联测试）。
     */
    private Long createTestUser(String username) {
        SysUserEntity user = new SysUserEntity();
        user.setTenantId("default");
        user.setUsername(username);
        user.setNickname(username);
        user.setPasswordHash("dummy");
        user.setUserType("USER");
        user.setStatus("ACTIVE");
        user.setNeedPasswordChange(false);
        user.setFailedLoginCount(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(0);
        userMapper.insert(user);
        return user.getId();
    }

    /**
     * Helper: 创建一个角色供分配使用。
     */
    private Long createTestRole(String code, String name) {
        SysRoleEntity role = new SysRoleEntity();
        role.setCode(code);
        role.setName(name);
        role.setStatus("ACTIVE");
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.insert(role);
        return role.getId();
    }

    @Test
    void assignRolesBatch_success() throws Exception {
        // 准备数据：用户、角色
        Long userId = createTestUser("user1");
        Long roleId = createTestRole("testrole", "测试角色");

        // 发送请求（使用拥有 system:user:assignRole 权限的 mock 用户）
        String payload = objectMapper.writeValueAsString(
                Map.of("userIds", Collections.singletonList(userId),
                       "roleIds", Collections.singletonList(roleId)));

        String response = mockMvc.perform(put("/api/user/roles")
                        .with(user("admin").authorities(() -> "system:user:assignRole"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertThat(root.get("code").asText()).isEqualTo("0");

        // 验证关联已写入
        assertThat(userRoleMapper.selectCount(null)).isEqualTo(1);
    }

    @Test
    void assignRolesBatch_clearRoles() throws Exception {
        Long userId = createTestUser("user2");
        Long roleId = createTestRole("testrole2", "测试角色2");
        // 先分配一次
        userRoleMapper.insert(new com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserRoleEntity(){
            {
                setUserId(userId);
                setRoleId(roleId);
                setCreatedAt(LocalDateTime.now());
            }
        });

        // 再次调用，roleIds 为空 => 清除所有角色
        String payload = objectMapper.writeValueAsString(
                Map.of("userIds", Collections.singletonList(userId),
                       "roleIds", Collections.emptyList()));

        String response = mockMvc.perform(put("/api/user/roles")
                        .with(user("admin").authorities(() -> "system:user:assignRole"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertThat(root.get("code").asText()).isEqualTo("0");
        // 关联应已被删除
        assertThat(userRoleMapper.selectCount(null)).isEqualTo(0);
    }

    @Test
    void assignRolesBatch_missingUserIds() throws Exception {
        String payload = objectMapper.writeValueAsString(
                Map.of("userIds", Collections.emptyList(),
                       "roleIds", Collections.emptyList()));

        String response = mockMvc.perform(put("/api/user/roles")
                        .with(user("admin").authorities(() -> "system:user:assignRole"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        assertThat(root.get("code").asText()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void assignRolesBatch_unknownRole() throws Exception {
        Long userId = createTestUser("user3");
        // roleId 999 不存在
        String payload = objectMapper.writeValueAsString(
                Map.of("userIds", Collections.singletonList(userId),
                       "roleIds", Collections.singletonList(999L)));

        String response = mockMvc.perform(put("/api/user/roles")
                        .with(user("admin").authorities(() -> "system:user:assignRole"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        assertThat(root.get("code").asText()).isEqualTo("ROLE_NOT_FOUND");
    }
}