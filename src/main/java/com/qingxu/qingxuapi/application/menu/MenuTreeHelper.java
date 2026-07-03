package com.qingxu.qingxuapi.application.menu;

import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysMenuEntity;
import com.qingxu.qingxuapi.interfaces.auth.dto.MenuTreeResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MenuTreeHelper {

    /**
     * 构建完整菜单树（包含所有类型：目录、菜单、按钮）
     */
    public List<MenuTreeResponse> buildTree(List<SysMenuEntity> allMenus) {
        Map<Long, List<SysMenuEntity>> childrenMap = allMenus.stream()
                .collect(Collectors.groupingBy(SysMenuEntity::getParentId));

        return buildTreeRecursive(allMenus, childrenMap, 0L);
    }

    /**
     * 构建路由树（仅包含目录和菜单，不包含按钮）
     * 用于前端路由生成和导航菜单展示
     */
    public List<MenuTreeResponse> buildRouteTree(List<SysMenuEntity> allMenus) {
        List<SysMenuEntity> nonButtonMenus = allMenus.stream()
                .filter(menu -> !"BUTTON".equals(menu.getMenuType()))
                .toList();

        Map<Long, List<SysMenuEntity>> childrenMap = nonButtonMenus.stream()
                .collect(Collectors.groupingBy(SysMenuEntity::getParentId));

        return buildTreeRecursive(nonButtonMenus, childrenMap, 0L);
    }

    /**
     * 递归构建菜单树
     */
    private List<MenuTreeResponse> buildTreeRecursive(List<SysMenuEntity> allMenus,
                                                       Map<Long, List<SysMenuEntity>> childrenMap,
                                                       Long parentId) {
        List<SysMenuEntity> children = childrenMap.getOrDefault(parentId, new ArrayList<>());

        return children.stream()
                .sorted(Comparator.comparingInt(SysMenuEntity::getSortOrder))
                .map(menu -> {
                    List<MenuTreeResponse> childResponses = buildTreeRecursive(allMenus, childrenMap, menu.getId());
                    return convertToTreeNode(menu, childResponses);
                })
                .collect(Collectors.toList());
    }

    /**
     * 将实体转换为树节点
     */
    private MenuTreeResponse convertToTreeNode(SysMenuEntity menu, List<MenuTreeResponse> children) {
        return new MenuTreeResponse(
                menu.getId(),
                menu.getParentId(),
                menu.getName(),
                menu.getPath(),
                menu.getComponent(),
                menu.getRedirect(),
                menu.getTitle(),
                menu.getIcon(),
                menu.getPermission(),
                menu.getMenuType(),
                menu.getSortOrder(),
                menu.getVisible(),
                menu.getStatus(),
                menu.getIsExternal(),
                menu.getIsCache(),
                menu.getCreatedAt(),
                menu.getUpdatedAt(),
                children
        );
    }
}