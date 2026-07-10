package com.qingxu.qingxuapi.application.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingxu.qingxuapi.application.menu.MenuVisibilityService;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysMenuEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysRoleEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysRoleMenuEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserPermissionService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final MenuVisibilityService menuVisibilityService;

    public List<String> resolveActivePermissionCodes(Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<SysRoleEntity> activeRoles = roleMapper.selectActiveByUserId(userId);
        if (activeRoles.isEmpty()) {
            return List.of();
        }
        Set<Long> roleIds = activeRoles.stream()
                .map(SysRoleEntity::getId)
                .collect(java.util.stream.Collectors.toSet());
        List<SysRoleMenuEntity> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenuEntity>()
                        .in(SysRoleMenuEntity::getRoleId, roleIds)
        );
        if (roleMenus.isEmpty()) {
            return List.of();
        }
        Set<Long> assignedMenuIds = roleMenus.stream()
                .map(SysRoleMenuEntity::getMenuId)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> permissionCodes = new LinkedHashSet<>();
        for (SysMenuEntity menu : menuVisibilityService.loadVisibleMenus()) {
            if (assignedMenuIds.contains(menu.getId()) && StringUtils.hasText(menu.getPermission())) {
                permissionCodes.add(menu.getPermission());
            }
        }
        return List.copyOf(permissionCodes);
    }
}
