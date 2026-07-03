package com.qingxu.qingxuapi.interfaces.menu;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qingxu.qingxuapi.application.auth.AuthApplicationService;
import com.qingxu.qingxuapi.application.menu.MenuApplicationService;
import com.qingxu.qingxuapi.common.response.ApiResponse;
import com.qingxu.qingxuapi.common.response.ResponseFactory;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysMenuEntity;
import com.qingxu.qingxuapi.interfaces.auth.dto.MenuTreeResponse;
import com.qingxu.qingxuapi.interfaces.menu.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuApplicationService menuApplicationService;
    private final AuthApplicationService authApplicationService;
    private final ResponseFactory responseFactory;

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public ApiResponse<List<MenuTreeNode>> getTree(MenuTreeQueryRequest request, HttpServletRequest servletRequest) {
        authApplicationService.currentUser(servletRequest);
        return responseFactory.success(menuApplicationService.getMenuTree(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('system:menu:list')")
    public ApiResponse<Page<SysMenuEntity>> list(MenuQueryRequest request, HttpServletRequest servletRequest) {
        authApplicationService.currentUser(servletRequest);
        return responseFactory.success(menuApplicationService.getMenuPage(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public ApiResponse<SysMenuEntity> detail(@PathVariable Long id, HttpServletRequest servletRequest) {
        authApplicationService.currentUser(servletRequest);
        return responseFactory.success(menuApplicationService.getMenuDetail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:create')")
    public ApiResponse<MenuCreateResponse> create(@Valid @RequestBody MenuCreateRequest request,
                                                  HttpServletRequest servletRequest) {
        authApplicationService.currentUser(servletRequest);
        return responseFactory.success(menuApplicationService.createMenu(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:update')")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody MenuUpdateRequest request,
                                    HttpServletRequest servletRequest) {
        authApplicationService.currentUser(servletRequest);
        menuApplicationService.updateMenu(id, request);
        return responseFactory.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest servletRequest) {
        authApplicationService.currentUser(servletRequest);
        menuApplicationService.deleteMenu(id);
        return responseFactory.success(null);
    }

    @GetMapping("/routes")
    public ApiResponse<List<MenuTreeResponse>> getRoutes(HttpServletRequest servletRequest) {
        var currentUser = authApplicationService.currentUser(servletRequest);
        return responseFactory.success(menuApplicationService.getUserRoutes(currentUser.id()));
    }

    @GetMapping("/full-tree")
    public ApiResponse<List<MenuTreeResponse>> getFullMenuTree() {
        return responseFactory.success(menuApplicationService.getFullMenuTree());
    }
}