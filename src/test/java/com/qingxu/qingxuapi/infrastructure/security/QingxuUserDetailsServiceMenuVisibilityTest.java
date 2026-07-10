package com.qingxu.qingxuapi.infrastructure.security;

import com.qingxu.qingxuapi.application.auth.UserPermissionService;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysRoleEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QingxuUserDetailsServiceMenuVisibilityTest {

    @Test
    void loadUserByUsernameUsesActiveRolesAndDynamicMenuPermissions() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        UserPermissionService userPermissionService = mock(UserPermissionService.class);
        QingxuUserDetailsService service = new QingxuUserDetailsService(
                userMapper,
                roleMapper,
                userPermissionService
        );
        SysUserEntity user = new SysUserEntity();
        user.setId(7L);
        user.setUsername("alice");
        user.setStatus("ACTIVE");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(roleMapper.selectActiveByUserId(7L)).thenReturn(List.of(role("admin")));
        when(userPermissionService.resolveActivePermissionCodes(7L)).thenReturn(List.of(
                "system:department:list",
                "system:department:view"
        ));

        QingxuUserPrincipal principal = (QingxuUserPrincipal) service.loadUserByUsername("alice");

        assertThat(principal.getRoles()).containsExactly("admin");
        assertThat(principal.getPermissions()).containsExactly("system:department:list", "system:department:view");
        assertThat(principal.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactlyInAnyOrder("ROLE_admin", "system:department:list", "system:department:view");
    }

    private SysRoleEntity role(String code) {
        SysRoleEntity role = new SysRoleEntity();
        role.setCode(code);
        return role;
    }
}
