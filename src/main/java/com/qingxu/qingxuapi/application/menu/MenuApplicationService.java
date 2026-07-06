package com.qingxu.qingxuapi.application.menu;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qingxu.qingxuapi.common.exception.BusinessException;
import com.qingxu.qingxuapi.common.permissionchange.PermissionChangeDispatcher;
import com.qingxu.qingxuapi.common.response.ErrorCode;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysMenuEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysRoleEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysRoleMenuEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysMenuMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMenuMapper;
import com.qingxu.qingxuapi.interfaces.auth.dto.MenuTreeResponse;
import com.qingxu.qingxuapi.interfaces.menu.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuApplicationService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final MenuTreeHelper menuTreeHelper;
    private final PermissionChangeDispatcher permissionChangeDispatcher;

    public List<MenuTreeNode> getMenuTree(MenuTreeQueryRequest request) {
        if (request != null && hasFilter(request)) {
            List<SysMenuEntity> list = queryFilteredMenus(request);
            return list.stream().map(this::convertToFlatMenuTreeNode).toList();
        }
        List<SysMenuEntity> allMenus = queryAllMenus();
        return menuTreeHelper.buildTree(allMenus).stream()
                .map(this::convertToMenuTreeNode)
                .toList();
    }

    private List<SysMenuEntity> queryAllMenus() {
        return menuMapper.selectList(
                new LambdaQueryWrapper<SysMenuEntity>()
                        .orderByAsc(SysMenuEntity::getSortOrder));
    }

    private boolean hasFilter(MenuTreeQueryRequest request) {
        return StringUtils.hasText(request.getMenuType())
                || StringUtils.hasText(request.getName())
                || StringUtils.hasText(request.getTitle())
                || StringUtils.hasText(request.getStatus())
                || request.getVisible() != null;
    }

    private List<SysMenuEntity> queryFilteredMenus(MenuTreeQueryRequest request) {
        LambdaQueryWrapper<SysMenuEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysMenuEntity::getSortOrder);

        if (StringUtils.hasText(request.getMenuType())) {
            wrapper.eq(SysMenuEntity::getMenuType, request.getMenuType());
        }
        if (StringUtils.hasText(request.getName())) {
            wrapper.like(SysMenuEntity::getName, request.getName());
        }
        if (StringUtils.hasText(request.getTitle())) {
            wrapper.like(SysMenuEntity::getTitle, request.getTitle());
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(SysMenuEntity::getStatus, request.getStatus());
        }
        if (request.getVisible() != null) {
            wrapper.eq(SysMenuEntity::getVisible, request.getVisible());
        }
        if (request.getParentId() != null) {
            wrapper.eq(SysMenuEntity::getParentId, request.getParentId());
        }

        return menuMapper.selectList(wrapper);
    }

    private MenuTreeNode convertToFlatMenuTreeNode(SysMenuEntity menu) {
        return new MenuTreeNode(
                menu.getId(), menu.getParentId(),
                menu.getName(), menu.getPath(), menu.getComponent(), menu.getRedirect(),
                menu.getTitle(), menu.getIcon(), menu.getPermission(), menu.getMenuType(),
                menu.getSortOrder(), menu.getVisible(), menu.getStatus(),
                menu.getIsExternal(), menu.getIsCache(),
                menu.getCreatedAt(), menu.getUpdatedAt(), List.of()
        );
    }

    /**
     * 鑾峰彇鑿滃崟鍒楄〃锛堝钩閾猴級
     */
    public Page<SysMenuEntity> getMenuPage(MenuQueryRequest request) {
        LambdaQueryWrapper<SysMenuEntity> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(request.getName())) {
            wrapper.like(SysMenuEntity::getName, request.getName());
        }
        if (StringUtils.hasText(request.getTitle())) {
            wrapper.like(SysMenuEntity::getTitle, request.getTitle());
        }
        if (request.getStatus() != null) {
            wrapper.eq(SysMenuEntity::getStatus, request.getStatus());
        }
        if (request.getMenuType() != null) {
            wrapper.eq(SysMenuEntity::getMenuType, request.getMenuType());
        }
        if (request.getParentId() != null) {
            wrapper.eq(SysMenuEntity::getParentId, request.getParentId());
        }

        wrapper.orderByAsc(SysMenuEntity::getSortOrder);

        return menuMapper.selectPage(
                new Page<>(request.getPage(), request.getPageSize()),
                wrapper
        );
    }

    /**
     * 鑾峰彇鑿滃崟璇︽儏
     */
    public SysMenuEntity getMenuDetail(Long id) {
        SysMenuEntity menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }
        return menu;
    }

    /**
     * 鍒涘缓鑿滃崟
     */
    @Transactional
    public MenuCreateResponse createMenu(MenuCreateRequest request) {
        validateParentMenu(request.getParentId(), request.getMenuType());
        validateMenuNameUnique(request.getName(), null);
        validateMenuTypeConstraints(request.getMenuType(), request.getComponent());
        validatePath(request.getMenuType(), request.getPath());

        SysMenuEntity menu = new SysMenuEntity();
        BeanUtils.copyProperties(request, menu);
        if ("BUTTON".equals(request.getMenuType()) && menu.getPath() == null) {
            menu.setPath("");
        }
        menu.setCreatedAt(LocalDateTime.now());
        menu.setUpdatedAt(LocalDateTime.now());
        menuMapper.insert(menu);

        return new MenuCreateResponse(menu.getId());
    }

    /**
     * 鏇存柊鑿滃崟
     */
    @Transactional
    public void updateMenu(Long id, MenuUpdateRequest request) {
        SysMenuEntity existing = menuMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }

        String oldStatus = existing.getStatus();

        // Home 鑿滃崟淇濇姢
        if ("Home".equals(existing.getName()) && existing.getParentId() == 0L) {
            if (request.getStatus() != null && !request.getStatus().equals(existing.getStatus())) {
                throw new BusinessException(ErrorCode.MENU_HOME_CANNOT_MODIFY_STATUS);
            }
        }

        validateParentMenu(request.getParentId(), request.getMenuType());
        validateMenuNameUnique(request.getName(), id);
        validateMenuTypeConstraints(request.getMenuType(), request.getComponent());
        validatePath(request.getMenuType(), request.getPath());

        BeanUtils.copyProperties(request, existing);
        if ("BUTTON".equals(request.getMenuType()) && existing.getPath() == null) {
            existing.setPath("");
        }
        existing.setUpdatedAt(LocalDateTime.now());
        menuMapper.updateById(existing);

        String reason = (request.getStatus() != null && !request.getStatus().equals(oldStatus))
                ? "菜单状态变更: " + oldStatus + " -> " + request.getStatus()
                : "菜单内容变更";
        permissionChangeDispatcher.fireMenuChange(id, null, null, reason);
    }

    /**
     * 鍒犻櫎鑿滃崟
     */
    @Transactional
    public void deleteMenu(Long id) {
        SysMenuEntity existing = menuMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }

        // Home 鑿滃崟淇濇姢
        if ("Home".equals(existing.getName()) && existing.getParentId() == 0L) {
            throw new BusinessException(ErrorCode.MENU_HOME_CANNOT_DELETE);
        }

        // 检查是否有子菜单
        long childCount = menuMapper.countByParentId(id);
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.MENU_HAS_CHILDREN);
        }

        permissionChangeDispatcher.fireMenuChange(id, null, null, "菜单被删除");
        menuMapper.deleteById(id);
    }

    /**
     * 获取用户路由（登录后下发，不包含按钮）
     */
    public List<MenuTreeResponse> getUserRoutes(Long userId) {
        Set<Long> allowedMenuIds = getUserAllowedMenuIds(userId);
        return getRouteTreeFiltered(allowedMenuIds);
    }

    /**
     * 获取用户菜单（登录后下发，不包含按钮）
     */
    public List<MenuTreeResponse> getUserMenus(Long userId) {
        Set<Long> allowedMenuIds = getUserAllowedMenuIds(userId);
        return getRouteTreeFiltered(allowedMenuIds);
    }

    private Set<Long> getUserAllowedMenuIds(Long userId) {
        List<SysRoleEntity> roles = roleMapper.selectByUserId(userId);
        if (roles.isEmpty()) {
            return Set.of();
        }
        List<Long> roleIds = roles.stream().map(SysRoleEntity::getId).toList();
        List<SysRoleMenuEntity> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenuEntity>()
                        .in(SysRoleMenuEntity::getRoleId, roleIds));
        return roleMenus.stream().map(SysRoleMenuEntity::getMenuId).collect(Collectors.toSet());
    }

    private List<MenuTreeResponse> getRouteTreeFiltered(Set<Long> allowedMenuIds) {
        List<SysMenuEntity> allMenus = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenuEntity>()
                        .orderByAsc(SysMenuEntity::getSortOrder));
        if (allowedMenuIds.isEmpty()) {
            return menuTreeHelper.buildRouteTree(allMenus);
        }
        List<SysMenuEntity> filteredMenus = allMenus.stream()
                .filter(menu -> allowedMenuIds.contains(menu.getId()))
                .toList();
        Set<Long> reachableIds = collectReachableParentIds(filteredMenus, allMenus);
        List<SysMenuEntity> resultMenus = allMenus.stream()
                .filter(menu -> allowedMenuIds.contains(menu.getId()) || reachableIds.contains(menu.getId()))
                .toList();
        return menuTreeHelper.buildRouteTree(resultMenus);
    }

    private Set<Long> collectReachableParentIds(List<SysMenuEntity> filteredMenus, List<SysMenuEntity> allMenus) {
        Map<Long, SysMenuEntity> menuMap = allMenus.stream()
                .collect(Collectors.toMap(SysMenuEntity::getId, m -> m));
        Set<Long> reachable = new HashSet<>();
        for (SysMenuEntity menu : filteredMenus) {
            Long parentId = menu.getParentId();
            while (parentId != null && parentId != 0L) {
                if (reachable.contains(parentId)) {
                    break;
                }
                reachable.add(parentId);
                SysMenuEntity parent = menuMap.get(parentId);
                if (parent == null) {
                    break;
                }
                parentId = parent.getParentId();
            }
        }
        return reachable;
    }

    /**
     * 获取完整菜单树（登录后下发，包含所有类型）
     */
    public List<MenuTreeResponse> getFullMenuTree() {
        List<SysMenuEntity> allMenus = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenuEntity>()
                        .orderByAsc(SysMenuEntity::getSortOrder)
        );
        return menuTreeHelper.buildTree(allMenus);
    }

    /**
     * 获取路由树（不包含按钮，用于前端路由和导航）
     */
    private List<MenuTreeResponse> getRouteTreeWithoutButtons() {
        List<SysMenuEntity> allMenus = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenuEntity>()
                        .orderByAsc(SysMenuEntity::getSortOrder)
        );
        return menuTreeHelper.buildRouteTree(allMenus);
    }

    // ========== 绉佹湁鏂规硶锛氭牎楠?==========

    private void validateParentMenu(Long parentId, String childMenuType) {
        if (parentId == null || parentId == 0L) return;

        SysMenuEntity parent = menuMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(ErrorCode.MENU_PARENT_NOT_FOUND);
        }

        String parentType = parent.getMenuType();

        if ("BUTTON".equals(parentType)) {
            throw new BusinessException(ErrorCode.MENU_PARENT_TYPE_ERROR);
        }

        if ("MENU".equals(parentType) && !"BUTTON".equals(childMenuType)) {
            throw new BusinessException(ErrorCode.MENU_PARENT_TYPE_ERROR);
        }
    }

    private void validateMenuNameUnique(String name, Long excludeId) {
        SysMenuEntity existing = menuMapper.findByName(name);
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw new BusinessException(ErrorCode.MENU_NAME_DUPLICATE);
        }
    }

    private void validateMenuTypeConstraints(String menuType, String component) {
        if ("MENU".equals(menuType)
                && (component == null || component.isBlank())) {
            throw new BusinessException(ErrorCode.MENU_COMPONENT_REQUIRED);
        }
    }

    /**
     * 校验路由路径。MENU、DIRECTORY 必须提供非空路径，BUTTON 可为空（后端会补空字符串）。
     */
    private void validatePath(String menuType, String path) {
        if (!"BUTTON".equals(menuType) && !StringUtils.hasText(path)) {
            // 使用缺少必填参数错误码
            throw new BusinessException(ErrorCode.MISSING_PARAMETER);
        }
    }

    private MenuTreeNode convertToMenuTreeNode(MenuTreeResponse response) {
        List<MenuTreeNode> children = response.children() != null
                ? response.children().stream().map(this::convertToMenuTreeNode).toList()
                : List.of();

        return new MenuTreeNode(
                response.id(),
                response.parentId(),
                response.name(),
                response.path(),
                response.component(),
                response.redirect(),
                response.title(),
                response.icon(), response.permission(), response.menuType(),
                response.sortOrder(),
                response.visible(),
                response.status(),
                response.isExternal(),
                response.isCache(),
                response.createdAt(),
                response.updatedAt(),
                children
        );
    }
}