package com.qingxu.qingxuapi.application.role;

import com.qingxu.qingxuapi.common.exception.BusinessException;
import com.qingxu.qingxuapi.common.permissionchange.PermissionChangeDispatcher;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysMenuEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysRoleEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysMenuMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMenuMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleApplicationServiceTest {

    @Test
    void assignMenusReportsMissingMenuIds() {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysRoleMenuMapper roleMenuMapper = mock(SysRoleMenuMapper.class);
        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        PermissionChangeDispatcher permissionChangeDispatcher = mock(PermissionChangeDispatcher.class);
        RoleApplicationService service = new RoleApplicationService(
                roleMapper,
                roleMenuMapper,
                menuMapper,
                userRoleMapper,
                permissionChangeDispatcher
        );

        SysRoleEntity role = new SysRoleEntity();
        role.setId(3L);
        when(roleMapper.selectById(3L)).thenReturn(role);
        SysMenuEntity menu = new SysMenuEntity();
        menu.setId(1L);
        when(menuMapper.selectList(any())).thenReturn(List.of(menu));

        assertThatThrownBy(() -> service.assignMenus(3L, List.of(1L, 5L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("5");
    }
}
