package com.qingxu.qingxuapi.application.role;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qingxu.qingxuapi.common.exception.BusinessException;
import com.qingxu.qingxuapi.common.permissionchange.PermissionChangeDispatcher;
import com.qingxu.qingxuapi.common.response.ErrorCode;
import com.qingxu.qingxuapi.common.response.PageResponse;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysMenuEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysRoleEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysRoleMenuEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserRoleEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysMenuMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMenuMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserRoleMapper;
import com.qingxu.qingxuapi.interfaces.role.dto.CreateRoleRequest;
import com.qingxu.qingxuapi.interfaces.role.dto.RoleResponse;
import com.qingxu.qingxuapi.interfaces.role.dto.UpdateRoleRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleApplicationService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PermissionChangeDispatcher permissionChangeDispatcher;

    public PageResponse<RoleResponse> getRolePage(String code, String name, String status, int current, int pageSize) {
        LambdaQueryWrapper<SysRoleEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(code)) {
            wrapper.like(SysRoleEntity::getCode, code);
        }
        if (StringUtils.hasText(name)) {
            wrapper.like(SysRoleEntity::getName, name);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysRoleEntity::getStatus, status);
        }
        wrapper.orderByAsc(SysRoleEntity::getSortOrder);
        Page<SysRoleEntity> page = roleMapper.selectPage(new Page<>(current, pageSize), wrapper);
        List<RoleResponse> records = page.getRecords().stream()
                .map(this::convertToResponse)
                .toList();
        return new PageResponse<>(records, page.getTotal(), current, pageSize);
    }

    public RoleResponse getRoleDetail(Long id) {
        SysRoleEntity role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        return convertToResponse(role);
    }

    @Transactional
    public Long createRole(CreateRoleRequest request) {
        validateCodeUnique(request.code(), null);
        SysRoleEntity role = new SysRoleEntity();
        role.setCode(request.code());
        role.setName(request.name());
        role.setStatus(request.status() != null ? request.status() : "ACTIVE");
        role.setDescription(request.description());
        role.setRemark(request.remark());
        role.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.insert(role);
        assignMenus(role.getId(), List.of(getHomeMenuId()));
        return role.getId();
    }

    @Transactional
    public void updateRole(Long id, UpdateRoleRequest request) {
        SysRoleEntity existing = roleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        existing.setName(request.name());
        existing.setStatus(request.status());
        existing.setDescription(request.description());
        existing.setRemark(request.remark());
        existing.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        existing.setUpdatedAt(LocalDateTime.now());
        roleMapper.updateById(existing);
    }

    @Transactional
    public void deleteRole(Long id) {
        SysRoleEntity role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        if ("admin".equals(role.getCode())) {
            throw new BusinessException(ErrorCode.ROLE_ADMIN_CANNOT_DELETE);
        }
        long userCount = userRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRoleEntity>()
                        .eq(SysUserRoleEntity::getRoleId, id)
        );
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.ROLE_HAS_USERS, "该角色下存在 " + userCount + " 个关联用户，不可删除");
        }
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRoleEntity>()
                .eq(SysUserRoleEntity::getRoleId, id));
        roleMenuMapper.deleteByRoleId(id);

        permissionChangeDispatcher.fireRoleChange(id, null, null, "角色被删除");
        roleMapper.deleteById(id);
    }

    @Transactional
    public void updateRoleStatus(Long id, String status) {
        SysRoleEntity role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        if ("admin".equals(role.getCode()) && "INACTIVE".equals(status)) {
            throw new BusinessException(ErrorCode.ROLE_ADMIN_CANNOT_DISABLE);
        }
        role.setStatus(status);
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.updateById(role);

        permissionChangeDispatcher.fireRoleChange(id, null, null, "角色状态变更: " + status);
    }

    public List<Long> getRoleMenuIds(Long id) {
        SysRoleEntity role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        return roleMenuMapper.selectMenuIdsByRoleId(id);
    }

    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRoleEntity role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        if (menuIds == null || menuIds.isEmpty()) {
            roleMenuMapper.deleteByRoleId(roleId);
            permissionChangeDispatcher.fireRoleChange(roleId, null, null, "清空角色菜单");
            return;
        }
        List<Long> distinctMenuIds = new LinkedHashSet<>(menuIds).stream().toList();
        List<SysMenuEntity> menus = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenuEntity>().in(SysMenuEntity::getId, distinctMenuIds)
        );
        Set<Long> existingMenuIds = menus.stream()
                .map(SysMenuEntity::getId)
                .collect(Collectors.toSet());
        List<Long> missingMenuIds = distinctMenuIds.stream()
                .filter(menuId -> !existingMenuIds.contains(menuId))
                .toList();
        if (!missingMenuIds.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.ROLE_MENU_NOT_FOUND,
                    ErrorCode.ROLE_MENU_NOT_FOUND.getMessage() + ": " + missingMenuIds
            );
        }

        roleMenuMapper.deleteByRoleId(roleId);
        List<SysRoleMenuEntity> roleMenuList = distinctMenuIds.stream().map(menuId -> {
            SysRoleMenuEntity roleMenu = new SysRoleMenuEntity();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            return roleMenu;
        }).toList();
        roleMenuMapper.batchInsert(roleMenuList);

        permissionChangeDispatcher.fireRoleChange(roleId, null, null, "重新分配角色菜单");
    }

    public List<Long> getMenuIdsByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return roleMenuMapper.selectList(
                        new LambdaQueryWrapper<SysRoleMenuEntity>()
                                .in(SysRoleMenuEntity::getRoleId, roleIds)
                ).stream()
                .map(SysRoleMenuEntity::getMenuId)
                .distinct()
                .toList();
    }

    private Long getHomeMenuId() {
        SysMenuEntity home = menuMapper.selectOne(
                new LambdaQueryWrapper<SysMenuEntity>()
                        .eq(SysMenuEntity::getName, "Home")
                        .or()
                        .eq(SysMenuEntity::getPath, "/home")
        );
        if (home == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }
        return home.getId();
    }

    private void validateCodeUnique(String code, Long excludeId) {
        SysRoleEntity existing = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRoleEntity>()
                        .eq(SysRoleEntity::getCode, code)
        );
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw new BusinessException(ErrorCode.ROLE_CODE_DUPLICATE);
        }
    }

    private RoleResponse convertToResponse(SysRoleEntity entity) {
        return new RoleResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getStatus(),
                entity.getDescription(),
                entity.getRemark(),
                entity.getSortOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
