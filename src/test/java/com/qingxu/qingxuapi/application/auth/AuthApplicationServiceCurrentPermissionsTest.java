package com.qingxu.qingxuapi.application.auth;

import com.qingxu.qingxuapi.common.audit.AuditService;
import com.qingxu.qingxuapi.application.user.UserApplicationService;
import com.qingxu.qingxuapi.infrastructure.captcha.CaptchaService;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysRoleEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserMapper;
import com.qingxu.qingxuapi.interfaces.auth.dto.CurrentUserResponse;
import com.qingxu.qingxuapi.interfaces.auth.dto.LoginRequest;
import com.qingxu.qingxuapi.interfaces.auth.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthApplicationServiceCurrentPermissionsTest {

    @Test
    void currentUserPermissionsDoesNotReturnStaleSessionPermissionsFromInactiveRoles() {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        UserPermissionService userPermissionService = mock(UserPermissionService.class);
        AuthApplicationService service = service(roleMapper, userPermissionService);
        HttpServletRequest request = mockRequestWithCurrentUser(currentUser());
        when(roleMapper.selectActiveByUserId(7L)).thenReturn(List.of());
        when(userPermissionService.resolveActivePermissionCodes(7L)).thenReturn(List.of());

        var response = service.currentUserPermissions(request);

        assertThat(response.roles()).isEmpty();
        assertThat(response.permissions()).isEmpty();
    }

    @Test
    void currentUserPermissionsReturnDynamicPermissionsFromRoleMenus() {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        UserPermissionService userPermissionService = mock(UserPermissionService.class);
        AuthApplicationService service = service(roleMapper, userPermissionService);
        HttpServletRequest request = mockRequestWithCurrentUser(currentUser());
        when(roleMapper.selectActiveByUserId(7L)).thenReturn(List.of(role("admin")));
        when(userPermissionService.resolveActivePermissionCodes(7L)).thenReturn(List.of(
                "system:department:list",
                "system:department:create",
                "system:department:update",
                "system:department:delete"
        ));

        var response = service.currentUserPermissions(request);

        assertThat(response.roles()).containsExactly("admin");
        assertThat(response.permissions()).containsExactly(
                "system:department:list",
                "system:department:create",
                "system:department:update",
                "system:department:delete"
        );
    }

    @Test
    void currentUserResponseDoesNotExposeRolesOrPermissions() {
        assertThat(Arrays.stream(CurrentUserResponse.class.getRecordComponents()).map(component -> component.getName()))
                .doesNotContain("roles", "permissions");
    }

    @Test
    void currentUserResponseCarriesAvatarForCurrentUserApi() {
        assertThat(currentUser().avatar()).isEqualTo("/api/files/123/download");
    }

    @Test
    void loginStoresDynamicAuthoritiesAndCurrentUserResponseDoesNotExposePermissions() {
        CaptchaService captchaService = mock(CaptchaService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SessionRegistry sessionRegistry = mock(SessionRegistry.class);
        UserPermissionService userPermissionService = mock(UserPermissionService.class);
        AuthApplicationService service = new AuthApplicationService(
                captchaService,
                passwordEncoder,
                mock(AuditService.class),
                userMapper,
                roleMapper,
                sessionRegistry,
                userPermissionService,
                null
        );
        SysUserEntity user = new SysUserEntity();
        user.setId(7L);
        user.setUsername("alice");
        user.setPasswordHash("encoded");
        user.setStatus("ACTIVE");
        user.setFailedLoginCount(0);
        user.setNeedPasswordChange(false);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("Password1!", "encoded")).thenReturn(true);
        when(roleMapper.selectActiveByUserId(7L)).thenReturn(List.of(role("admin")));
        when(userPermissionService.resolveActivePermissionCodes(7L)).thenReturn(List.of(
                "system:department:list",
                "system:department:create"
        ));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(true)).thenReturn(session);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        CurrentUserResponse response = service.login(new LoginRequest("alice", "Password1!", "key", "code"), request);

        assertThat(Arrays.stream(response.getClass().getRecordComponents()).map(component -> component.getName()))
                .doesNotContain("roles", "permissions");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactlyInAnyOrder("system:department:list", "system:department:create");
        verify(sessionRegistry).registerNewSession(any(), any());
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerAssignsDefaultUserRoleAfterCreatingPendingUser() {
        CaptchaService captchaService = mock(CaptchaService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        UserApplicationService userApplicationService = mock(UserApplicationService.class);
        AuthApplicationService service = new AuthApplicationService(
                captchaService,
                passwordEncoder,
                mock(AuditService.class),
                userMapper,
                mock(SysRoleMapper.class),
                mock(SessionRegistry.class),
                mock(UserPermissionService.class),
                userApplicationService
        );
        when(passwordEncoder.encode("Password1!")).thenReturn("encoded");
        when(userMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            SysUserEntity user = invocation.getArgument(0);
            user.setId(21L);
            return 1;
        }).when(userMapper).insert(any(SysUserEntity.class));

        service.register(new RegisterRequest(
                "alice",
                "Alice",
                "alice@example.com",
                "",
                "Password1!",
                "Password1!",
                "key",
                "code",
                true
        ), mock(HttpServletRequest.class));

        verify(userApplicationService).joinRootTempDept(21L);
        verify(userApplicationService).assignDefaultUserRole(21L);
    }

    private AuthApplicationService service(
            SysRoleMapper roleMapper,
            UserPermissionService userPermissionService
    ) {
        return new AuthApplicationService(
                mock(CaptchaService.class),
                mock(PasswordEncoder.class),
                mock(AuditService.class),
                mock(SysUserMapper.class),
                roleMapper,
                mock(SessionRegistry.class),
                userPermissionService,
                null
        );
    }

    private HttpServletRequest mockRequestWithCurrentUser(CurrentUserResponse currentUser) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthApplicationService.SESSION_CURRENT_USER)).thenReturn(currentUser);
        return request;
    }

    private CurrentUserResponse currentUser() {
        return new CurrentUserResponse(
                7L,
                "alice",
                null,
                "Alice",
                "/api/files/123/download",
                null,
                null,
                "default",
                "INTERNAL",
                "ACTIVE",
                false
        );
    }

    private SysRoleEntity role(String code) {
        SysRoleEntity role = new SysRoleEntity();
        role.setCode(code);
        return role;
    }
}
