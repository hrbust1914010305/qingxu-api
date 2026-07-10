package com.qingxu.qingxuapi.application.menu;

import com.qingxu.qingxuapi.common.permissionchange.PermissionChangeDispatcher;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysMenuEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysRoleEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysRoleMenuEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysMenuMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMenuMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MenuApplicationServiceVisibilityTest {

    private final SysMenuMapper menuMapper = mock(SysMenuMapper.class);
    private final SysRoleMapper roleMapper = mock(SysRoleMapper.class);
    private final SysRoleMenuMapper roleMenuMapper = mock(SysRoleMenuMapper.class);
    private final MenuVisibilityService menuVisibilityService = new MenuVisibilityService(menuMapper);
    private final MenuApplicationService service = new MenuApplicationService(
            menuMapper,
            roleMapper,
            roleMenuMapper,
            new MenuTreeHelper(),
            menuVisibilityService,
            mock(PermissionChangeDispatcher.class)
    );

    @Test
    void userRoutesExcludeDisabledAncestorsAndDescendantsEvenWhenAssigned() {
        Long userId = 7L;
        when(roleMapper.selectByUserId(userId)).thenReturn(List.of(role(1L)));
        when(roleMenuMapper.selectList(any())).thenReturn(List.of(
                roleMenu(1L, 3L),
                roleMenu(1L, 5L)
        ));
        when(menuMapper.selectList(any())).thenReturn(List.of(
                menu(1L, 0L, "Root", "/root", "Layout", "DIRECTORY", "ACTIVE", null),
                menu(2L, 1L, "DisabledDirectory", "disabled", null, "DIRECTORY", "DISABLED", null),
                menu(3L, 2L, "HiddenChildMenu", "hidden", "hidden/index", "MENU", "ACTIVE", "hidden:list"),
                menu(4L, 1L, "ActiveDirectory", "active", null, "DIRECTORY", "ACTIVE", null),
                menu(5L, 4L, "VisibleChildMenu", "visible", "visible/index", "MENU", "ACTIVE", "visible:list")
        ));

        var routes = service.getUserRoutes(userId);

        assertThat(flattenNames(routes)).contains("Root", "ActiveDirectory", "VisibleChildMenu");
        assertThat(flattenNames(routes)).doesNotContain("DisabledDirectory", "HiddenChildMenu");
    }

    private List<String> flattenNames(List<com.qingxu.qingxuapi.interfaces.auth.dto.MenuTreeResponse> routes) {
        return routes.stream()
                .flatMap(route -> java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(route.name()),
                        flattenNames(route.children()).stream()
                ))
                .toList();
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

    private SysMenuEntity menu(Long id, Long parentId, String name, String path, String component,
                               String menuType, String status, String permission) {
        SysMenuEntity menu = new SysMenuEntity();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setName(name);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setTitle(name);
        menu.setMenuType(menuType);
        menu.setStatus(status);
        menu.setPermission(permission);
        menu.setVisible(true);
        menu.setIsExternal(false);
        menu.setIsCache(true);
        menu.setSortOrder(id.intValue());
        return menu;
    }
}
