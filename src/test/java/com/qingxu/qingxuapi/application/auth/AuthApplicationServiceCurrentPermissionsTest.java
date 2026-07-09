package com.qingxu.qingxuapi.application.auth;

import com.qingxu.qingxuapi.common.audit.AuditService;
import com.qingxu.qingxuapi.infrastructure.captcha.CaptchaService;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysPermissionMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserMapper;
import com.qingxu.qingxuapi.interfaces.auth.dto.CurrentUserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthApplicationServiceCurrentPermissionsTest {

    @Test
    void currentUserPermissionsDoesNotReturnStaleSessionPermissionsFromInactiveRoles() {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysPermissionMapper permissionMapper = mock(SysPermissionMapper.class);
        AuthApplicationService service = new AuthApplicationService(
                mock(CaptchaService.class),
                mock(PasswordEncoder.class),
                mock(AuditService.class),
                mock(SysUserMapper.class),
                roleMapper,
                permissionMapper,
                mock(SessionRegistry.class),
                null
        );
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        CurrentUserResponse cachedUser = new CurrentUserResponse(
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
                false,
                List.of("roleA"),
                List.of("permission:a")
        );
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(AuthApplicationService.SESSION_CURRENT_USER)).thenReturn(cachedUser);
        when(roleMapper.selectActiveByUserId(7L)).thenReturn(List.of());
        when(permissionMapper.selectActiveByUserId(7L)).thenReturn(List.of());

        var response = service.currentUserPermissions(request);

        assertThat(response.roles()).doesNotContain("roleA");
        assertThat(response.permissions()).doesNotContain("permission:a");
        assertThat(response.permissions()).doesNotContain("department:list", "system:menu:list");
    }

    @Test
    void currentUserResponseCarriesAvatarForCurrentUserApi() {
        CurrentUserResponse currentUser = new CurrentUserResponse(
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
                false,
                List.of(),
                List.of()
        );

        assertThat(currentUser.avatar()).isEqualTo("/api/files/123/download");
    }
}
