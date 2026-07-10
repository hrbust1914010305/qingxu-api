package com.qingxu.qingxuapi.application.menu;

import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysMenuEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysMenuMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MenuVisibilityServiceTest {

    private final MenuVisibilityService service = new MenuVisibilityService(mock(SysMenuMapper.class));

    @Test
    void visibleMenusExcludeDisabledNodeAndDescendants() {
        List<SysMenuEntity> menus = List.of(
                menu(1L, 0L, "Root", "DIRECTORY", "ACTIVE", null),
                menu(2L, 1L, "DisabledDirectory", "DIRECTORY", "DISABLED", null),
                menu(3L, 2L, "ChildMenu", "MENU", "ACTIVE", "child:list"),
                menu(4L, 3L, "ChildButton", "BUTTON", "ACTIVE", "child:add"),
                menu(5L, 1L, "SiblingMenu", "MENU", "ACTIVE", "sibling:list"),
                menu(6L, 5L, "DisabledButton", "BUTTON", "DISABLED", "sibling:delete")
        );

        List<SysMenuEntity> visibleMenus = service.filterVisibleMenus(menus);

        assertThat(visibleMenus).extracting(SysMenuEntity::getId).containsExactly(1L, 5L);
    }

    @Test
    void visiblePermissionCodesOnlyIncludeActiveBranches() {
        List<SysMenuEntity> menus = List.of(
                menu(1L, 0L, "Root", "DIRECTORY", "ACTIVE", null),
                menu(2L, 1L, "ActiveMenu", "MENU", "ACTIVE", "active:list"),
                menu(3L, 2L, "ActiveButton", "BUTTON", "ACTIVE", "active:add"),
                menu(4L, 1L, "DisabledMenu", "MENU", "DISABLED", "disabled:list"),
                menu(5L, 4L, "DisabledDescendantButton", "BUTTON", "ACTIVE", "disabled:add"),
                menu(6L, 2L, "DisabledButton", "BUTTON", "DISABLED", "active:delete")
        );

        Set<String> permissionCodes = service.visiblePermissionCodes(menus);

        assertThat(permissionCodes).containsExactlyInAnyOrder("active:list", "active:add");
    }

    @Test
    void filterMenuManagedPermissionCodesKeepsNonMenuPermissionsAndFiltersDisabledMenuPermissions() {
        List<SysMenuEntity> menus = List.of(
                menu(1L, 0L, "Root", "DIRECTORY", "ACTIVE", null),
                menu(2L, 1L, "ActiveMenu", "MENU", "ACTIVE", "active:list"),
                menu(3L, 1L, "DisabledMenu", "MENU", "DISABLED", "disabled:list")
        );

        List<String> filtered = service.filterMenuManagedPermissionCodes(
                List.of("active:list", "disabled:list", "department:list"),
                menus
        );

        assertThat(filtered).containsExactly("active:list", "department:list");
    }

    private SysMenuEntity menu(Long id, Long parentId, String name, String menuType, String status, String permission) {
        SysMenuEntity menu = new SysMenuEntity();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setName(name);
        menu.setMenuType(menuType);
        menu.setStatus(status);
        menu.setPermission(permission);
        menu.setSortOrder(id.intValue());
        return menu;
    }
}
