package com.qingxu.qingxuapi.application.auth;

import com.qingxu.qingxuapi.application.menu.MenuVisibilityService;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysMenuEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysRoleEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysRoleMenuEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMenuMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserPermissionServiceTest {

    @Test
    void resolveActivePermissionCodesUseVisibleAssignedMenusFromActiveRoles() {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysRoleMenuMapper roleMenuMapper = mock(SysRoleMenuMapper.class);
        MenuVisibilityService menuVisibilityService = mock(MenuVisibilityService.class);
        UserPermissionService service = new UserPermissionService(roleMapper, roleMenuMapper, menuVisibilityService);
        when(roleMapper.selectActiveByUserId(7L)).thenReturn(List.of(role(1L)));
        when(roleMenuMapper.selectList(any())).thenReturn(List.of(
                roleMenu(1L, 10L),
                roleMenu(1L, 100L),
                roleMenu(1L, 101L),
                roleMenu(1L, 999L)
        ));
        when(menuVisibilityService.loadVisibleMenus()).thenReturn(List.of(
                menu(10L, "system:department:list"),
                menu(100L, "system:department:create"),
                menu(101L, "system:department:update"),
                menu(200L, "system:user:update")
        ));

        assertThat(service.resolveActivePermissionCodes(7L)).containsExactly(
                "system:department:list",
                "system:department:create",
                "system:department:update"
        );
    }

    @Test
    void resolveActivePermissionCodesReturnEmptyWhenNoActiveRoles() {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysRoleMenuMapper roleMenuMapper = mock(SysRoleMenuMapper.class);
        MenuVisibilityService menuVisibilityService = mock(MenuVisibilityService.class);
        UserPermissionService service = new UserPermissionService(roleMapper, roleMenuMapper, menuVisibilityService);
        when(roleMapper.selectActiveByUserId(7L)).thenReturn(List.of());

        assertThat(service.resolveActivePermissionCodes(7L)).isEmpty();
    }

    private SysRoleEntity role(Long id) {
        SysRoleEntity role = new SysRoleEntity();
        role.setId(id);
        role.setCode("role-" + id);
        return role;
    }

    private SysRoleMenuEntity roleMenu(Long roleId, Long menuId) {
        SysRoleMenuEntity roleMenu = new SysRoleMenuEntity();
        roleMenu.setRoleId(roleId);
        roleMenu.setMenuId(menuId);
        return roleMenu;
    }

    private SysMenuEntity menu(Long id, String permission) {
        SysMenuEntity menu = new SysMenuEntity();
        menu.setId(id);
        menu.setPermission(permission);
        return menu;
    }
}
