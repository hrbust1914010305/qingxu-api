package com.qingxu.qingxuapi.application.menu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysMenuEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuVisibilityService {

    private static final String ACTIVE = "ACTIVE";

    private final SysMenuMapper menuMapper;

    public List<SysMenuEntity> loadVisibleMenus() {
        List<SysMenuEntity> allMenus = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenuEntity>()
                        .orderByAsc(SysMenuEntity::getSortOrder));
        return filterVisibleMenus(allMenus);
    }

    public List<SysMenuEntity> filterVisibleMenus(List<SysMenuEntity> allMenus) {
        Map<Long, SysMenuEntity> menuById = allMenus.stream()
                .collect(Collectors.toMap(SysMenuEntity::getId, menu -> menu, (left, right) -> left));
        return allMenus.stream()
                .filter(menu -> isVisibleBranch(menu, menuById, new HashSet<>()))
                .toList();
    }

    public Set<String> visiblePermissionCodes() {
        return visiblePermissionCodes(loadVisibleMenus());
    }

    public Set<String> visiblePermissionCodes(List<SysMenuEntity> menus) {
        return filterVisibleMenus(menus).stream()
                .map(SysMenuEntity::getPermission)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    public Set<String> allMenuPermissionCodes() {
        return allMenuPermissionCodes(loadAllMenus());
    }

    public Set<String> allMenuPermissionCodes(List<SysMenuEntity> menus) {
        return menus.stream()
                .map(SysMenuEntity::getPermission)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    public List<String> filterMenuManagedPermissionCodes(Collection<String> permissionCodes) {
        List<SysMenuEntity> allMenus = loadAllMenus();
        return filterMenuManagedPermissionCodes(permissionCodes, allMenus);
    }

    public List<String> filterMenuManagedPermissionCodes(Collection<String> permissionCodes, List<SysMenuEntity> menus) {
        Set<String> allMenuPermissions = allMenuPermissionCodes(menus);
        Set<String> visiblePermissions = visiblePermissionCodes(menus);
        return permissionCodes.stream()
                .filter(code -> !allMenuPermissions.contains(code) || visiblePermissions.contains(code))
                .toList();
    }

    private List<SysMenuEntity> loadAllMenus() {
        return menuMapper.selectList(
                new LambdaQueryWrapper<SysMenuEntity>()
                        .orderByAsc(SysMenuEntity::getSortOrder));
    }

    private boolean isVisibleBranch(SysMenuEntity menu, Map<Long, SysMenuEntity> menuById, Set<Long> visiting) {
        if (menu == null || menu.getId() == null || !ACTIVE.equals(menu.getStatus())) {
            return false;
        }
        Long parentId = menu.getParentId();
        if (parentId == null || parentId == 0L) {
            return true;
        }
        if (!visiting.add(menu.getId())) {
            return false;
        }
        SysMenuEntity parent = menuById.get(parentId);
        return isVisibleBranch(parent, menuById, visiting);
    }
}
