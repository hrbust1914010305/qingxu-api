package com.qingxu.qingxuapi.common.permissionchange;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysRoleEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysRoleMenuEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserRoleEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMenuMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 反查权限变更受影响的 userId 集合。
 * <p>
 * 依赖既有 Mapper，所有 SQL 通过 LambdaQueryWrapper 拼装。涉及 {@code sys_role} 表
 * 走硬删除（无 deleted 列），{@code sys_user} 表走 MyBatis-Plus 逻辑删除自动过滤。
 */
@Component
@RequiredArgsConstructor
public class AffectedUserResolver {

    private static final String ADMIN_ROLE_CODE = "admin";

    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;

    /**
     * T1 菜单变更：取绑定该菜单的所有角色 → 这些角色下的全体 ACTIVE 用户。
     */
    public Set<Long> resolveByMenu(Long menuId) {
        if (menuId == null) {
            return Collections.emptySet();
        }
        List<SysRoleMenuEntity> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenuEntity>()
                        .eq(SysRoleMenuEntity::getMenuId, menuId));
        if (roleMenus.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> roleIds = new HashSet<>();
        for (SysRoleMenuEntity rm : roleMenus) {
            roleIds.add(rm.getRoleId());
        }
        return findActiveUserIdsByRoleIds(roleIds);
    }

    /**
     * T2 角色变更：取该角色下所有 ACTIVE 用户。
     */
    public Set<Long> resolveByRole(Long roleId) {
        if (roleId == null) {
            return Collections.emptySet();
        }
        return findActiveUserIdsByRoleIds(Set.of(roleId));
    }

    /**
     * T3 用户角色变更：仅影响该用户本人（仅在 ACTIVE 时才需通知）。
     */
    public Set<Long> resolveByUser(Long userId) {
        if (userId == null || !isActiveUser(userId)) {
            return Collections.emptySet();
        }
        return Set.of(userId);
    }

    /**
     * T4/T5 用户禁用/删除：无论原 status，针对用户本人触发。
     * <p>
     * 注意：删除场景下用户记录可能已被逻辑删除，{@code isActiveUser} 会返回 false，
     * 但 T4/T5 仍需推送，故调用方应直接使用 {@code Set.of(userId)}，不走此方法。
     */
    public Set<Long> resolveByUserForce(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        return Set.of(userId);
    }

    /**
     * 判断目标角色是否为 admin 角色（用于豁免分支判定）。
     */
    public boolean isAdminRole(Long roleId) {
        if (roleId == null) {
            return false;
        }
        SysRoleEntity role = roleMapper.selectById(roleId);
        return role != null && ADMIN_ROLE_CODE.equals(role.getCode());
    }

    private Set<Long> findActiveUserIdsByRoleIds(Set<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<SysUserRoleEntity> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRoleEntity>()
                        .in(SysUserRoleEntity::getRoleId, roleIds));
        if (userRoles.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> candidateUserIds = new HashSet<>();
        for (SysUserRoleEntity ur : userRoles) {
            candidateUserIds.add(ur.getUserId());
        }
        // 批量查 ACTIVE 用户（MyBatis-Plus 自动过滤 deleted=1）
        List<SysUserEntity> users = userMapper.selectList(
                new LambdaQueryWrapper<SysUserEntity>()
                        .in(SysUserEntity::getId, candidateUserIds)
                        .eq(SysUserEntity::getStatus, "ACTIVE"));
        Set<Long> result = new HashSet<>();
        for (SysUserEntity u : users) {
            result.add(u.getId());
        }
        return result;
    }

    private boolean isActiveUser(Long userId) {
        if (userId == null) {
            return false;
        }
        SysUserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUserEntity>()
                        .eq(SysUserEntity::getId, userId)
                        .eq(SysUserEntity::getStatus, "ACTIVE"));
        return user != null;
    }
}