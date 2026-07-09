package com.qingxu.qingxuapi.application.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingxu.qingxuapi.common.audit.AuditService;
import com.qingxu.qingxuapi.common.permissionchange.PermissionChangeDispatcher;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysFileEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysRoleEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysDepartmentMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysFileMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserDepartmentMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserPreferenceMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserRoleMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserApplicationServiceAssignRolesTest {

    private final SysUserPreferenceMapper preferenceMapper = mock(SysUserPreferenceMapper.class);
    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SysUserDepartmentMapper userDeptMapper = mock(SysUserDepartmentMapper.class);
    private final SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
    private final SysDepartmentMapper deptMapper = mock(SysDepartmentMapper.class);
    private final SysRoleMapper roleMapper = mock(SysRoleMapper.class);
    private final SysFileMapper fileMapper = mock(SysFileMapper.class);
    private final AuditService auditService = mock(AuditService.class);
    private final SessionRegistry sessionRegistry = mock(SessionRegistry.class);
    private final PermissionChangeDispatcher permissionChangeDispatcher = mock(PermissionChangeDispatcher.class);
    private final HttpServletRequest servletRequest = mock(HttpServletRequest.class);

    private final UserApplicationService service = new UserApplicationService(
            preferenceMapper,
            userMapper,
            passwordEncoder,
            objectMapper,
            userDeptMapper,
            userRoleMapper,
            deptMapper,
            roleMapper,
            fileMapper,
            auditService,
            sessionRegistry,
            permissionChangeDispatcher
    );

    @Test
    void assignRolesBatchNotifiesUserWhenRolesAreCleared() {
        Long userId = 2L;
        when(userMapper.selectBatchIds(List.of(userId))).thenReturn(List.of(activeUser(userId)));

        service.assignRolesBatch(List.of(userId), List.of(), 1L, "admin", servletRequest);

        verify(permissionChangeDispatcher).fireUserRoleChange(
                eq(userId),
                eq(1L),
                eq("admin"),
                eq("用户角色分配变更")
        );
    }

    @Test
    void assignRolesBatchNotifiesEachUserAfterAssigningRoles() {
        Long userId1 = 2L;
        Long userId2 = 3L;
        Long roleId = 10L;
        when(userMapper.selectBatchIds(List.of(userId1, userId2)))
                .thenReturn(List.of(activeUser(userId1), activeUser(userId2)));
        when(roleMapper.selectList(any())).thenReturn(List.of(role(roleId)));

        service.assignRolesBatch(List.of(userId1, userId2), List.of(roleId), 1L, "admin", servletRequest);

        verify(permissionChangeDispatcher).fireUserRoleChange(userId1, 1L, "admin", "用户角色分配变更");
        verify(permissionChangeDispatcher).fireUserRoleChange(userId2, 1L, "admin", "用户角色分配变更");
    }

    @Test
    void assignRolesBatchDoesNotNotifyWhenValidationFails() {
        Long userId = 2L;
        Long missingRoleId = 404L;
        when(userMapper.selectBatchIds(List.of(userId))).thenReturn(List.of(activeUser(userId)));
        when(roleMapper.selectList(any())).thenReturn(List.of());

        try {
            service.assignRolesBatch(List.of(userId), List.of(missingRoleId), 1L, "admin", servletRequest);
        } catch (Exception ignored) {
        }

        verify(permissionChangeDispatcher, never()).fireUserRoleChange(any(), any(), any(), any());
    }

    @Test
    void detailReturnsAvatarAndAvatarFilesForUploadEcho() {
        Long userId = 2L;
        SysUserEntity user = activeUser(userId);
        user.setUsername("alice");
        user.setNickname("Alice");
        user.setUserType("INTERNAL");
        user.setAvatar("/api/files/123/download");
        when(userMapper.selectById(userId)).thenReturn(user);
        when(userDeptMapper.selectByUserId(userId)).thenReturn(List.of());
        when(roleMapper.selectByUserId(userId)).thenReturn(List.of());
        SysFileEntity avatarFile = new SysFileEntity();
        avatarFile.setId(123L);
        avatarFile.setOriginalName("alice-avatar.png");
        avatarFile.setSize(2048L);
        when(fileMapper.selectById(123L)).thenReturn(avatarFile);

        var response = service.detail(userId);

        assertThat(response.avatar()).isEqualTo("/api/files/123/download");
        assertThat(response.avatarFiles()).hasSize(1);
        assertThat(response.avatarFiles().get(0).id()).isEqualTo(123L);
        assertThat(response.avatarFiles().get(0).uid()).isEqualTo("avatar-123");
        assertThat(response.avatarFiles().get(0).name()).isEqualTo("alice-avatar.png");
        assertThat(response.avatarFiles().get(0).size()).isEqualTo(2048L);
        assertThat(response.avatarFiles().get(0).url()).isEqualTo("/api/files/123/download");
        assertThat(response.avatarFiles().get(0).status()).isEqualTo("success");
        assertThat(response.avatarFiles().get(0).percent()).isEqualTo(100);
    }

    @Test
    void createStoresAvatarUrlOnUser() {
        when(passwordEncoder.encode("Password1!")).thenReturn("encoded");
        var request = new com.qingxu.qingxuapi.interfaces.user.dto.CreateUserRequest(
                "alice",
                "Alice",
                "Alice",
                "/api/files/123/download",
                "",
                "",
                "INTERNAL",
                "Password1!",
                List.of(),
                List.of()
        );

        service.create(request, 1L, "admin", servletRequest);

        var userCaptor = forClass(SysUserEntity.class);
        verify(userMapper).insert(userCaptor.capture());
        assertThat(userCaptor.getValue().getAvatar()).isEqualTo("/api/files/123/download");
    }

    @Test
    void updateStoresAvatarUrlOnUser() {
        Long userId = 2L;
        SysUserEntity user = activeUser(userId);
        when(userMapper.selectById(userId)).thenReturn(user);
        var request = new com.qingxu.qingxuapi.interfaces.user.dto.UpdateUserRequest(
                null,
                null,
                "/api/files/456/download",
                null,
                null,
                null,
                null,
                null,
                null
        );

        service.update(userId, request, 1L, "admin", servletRequest);

        assertThat(user.getAvatar()).isEqualTo("/api/files/456/download");
        verify(userMapper).updateById(user);
    }

    private SysUserEntity activeUser(Long id) {
        SysUserEntity user = new SysUserEntity();
        user.setId(id);
        user.setStatus("ACTIVE");
        return user;
    }

    private SysRoleEntity role(Long id) {
        SysRoleEntity role = new SysRoleEntity();
        role.setId(id);
        role.setCode("role-" + id);
        role.setName("Role " + id);
        return role;
    }
}
