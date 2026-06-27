package com.qingxu.qingxuapi.application.menu;

import com.qingxu.qingxuapi.interfaces.auth.dto.MenuTreeResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MenuApplicationService {

    public List<MenuTreeResponse> getUserMenus(Long userId) {
        return getMockMenuTree();
    }

    public List<MenuTreeResponse> getFullMenuTree() {
        return getMockMenuTree();
    }

    private List<MenuTreeResponse> getMockMenuTree() {
        List<MenuTreeResponse> menus = new ArrayList<>();

        menus.add(new MenuTreeResponse(
                1L, 0L, "Home", "/home", "home/index", null,
                "首页", "icon-home",
                "MENU", 1,
                true, "ACTIVE", false, true, new ArrayList<>()
        ));

        List<MenuTreeResponse> systemChildren = new ArrayList<>();
        systemChildren.add(new MenuTreeResponse(
                3L, 2L, "SystemUser", "user", "user/index", null,
                "用户管理", "icon-user",
                "MENU", 1,
                true, "ACTIVE", false, true, new ArrayList<>()
        ));
        systemChildren.add(new MenuTreeResponse(
                4L, 2L, "SystemRole", "role", "role/index", null,
                "角色管理", "icon-team",
                "MENU", 2,
                true, "ACTIVE", false, true, new ArrayList<>()
        ));
        systemChildren.add(new MenuTreeResponse(
                5L, 2L, "SystemPermission", "permission", "permission/index", null,
                "权限管理", "icon-lock",
                "MENU", 3,
                true, "ACTIVE", false, true, new ArrayList<>()
        ));

        menus.add(new MenuTreeResponse(
                2L, 0L, "System", "/system", "", "/system/user",
                "系统管理", "icon-settings",
                "DIRECTORY", 2,
                true, "ACTIVE", false, true, systemChildren
        ));

        List<MenuTreeResponse> knowledgeChildren = new ArrayList<>();
        knowledgeChildren.add(new MenuTreeResponse(
                7L, 6L, "KnowledgeList", "list", "knowledge/list/index", null,
                "知识库列表", "icon-list",
                "MENU", 1,
                true, "ACTIVE", false, true, new ArrayList<>()
        ));
        knowledgeChildren.add(new MenuTreeResponse(
                8L, 6L, "DocumentList", "document", "knowledge/document/index", null,
                "文档管理", "icon-file",
                "MENU", 2,
                true, "ACTIVE", false, true, new ArrayList<>()
        ));

        menus.add(new MenuTreeResponse(
                6L, 0L, "Knowledge", "/knowledge", "", "/knowledge/list",
                "知识库管理", "icon-folder",
                "DIRECTORY", 3,
                true, "ACTIVE", false, true, knowledgeChildren
        ));

        return menus;
    }
}