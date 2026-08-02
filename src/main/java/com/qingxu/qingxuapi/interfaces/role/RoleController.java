package com.qingxu.qingxuapi.interfaces.role;

import com.qingxu.qingxuapi.application.role.RoleApplicationService;
import com.qingxu.qingxuapi.common.response.ApiResponse;
import com.qingxu.qingxuapi.common.response.PageResponse;
import com.qingxu.qingxuapi.common.response.ResponseFactory;
import com.qingxu.qingxuapi.interfaces.role.dto.AssignMenusRequest;
import com.qingxu.qingxuapi.interfaces.role.dto.CreateRoleRequest;
import com.qingxu.qingxuapi.interfaces.role.dto.RoleResponse;
import com.qingxu.qingxuapi.interfaces.role.dto.UpdateRoleRequest;
import com.qingxu.qingxuapi.interfaces.role.dto.UpdateRoleStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleApplicationService roleApplicationService;
    private final ResponseFactory responseFactory;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:role:list')")
    public ApiResponse<PageResponse<RoleResponse>> list(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<RoleResponse> page = roleApplicationService.getRolePage(code, name, status, current, pageSize);
        return responseFactory.success(page);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:list')")
    public ApiResponse<RoleResponse> detail(@PathVariable Long id) {
        RoleResponse role = roleApplicationService.getRoleDetail(id);
        return responseFactory.success(role);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:role:create')")
    public ApiResponse<Map<String, Long>> create(@RequestBody @Valid CreateRoleRequest request) {
        Long id = roleApplicationService.createRole(request);
        return responseFactory.id(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:update')")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @RequestBody @Valid UpdateRoleRequest request) {
        roleApplicationService.updateRole(id, request);
        return responseFactory.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleApplicationService.deleteRole(id);
        return responseFactory.success();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:role:update')")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @RequestBody @Valid UpdateRoleStatusRequest request) {
        roleApplicationService.updateRoleStatus(id, request.status());
        return responseFactory.success();
    }

    @GetMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('system:role:list')")
    public ApiResponse<List<Long>> getMenus(@PathVariable Long id) {
        List<Long> menuIds = roleApplicationService.getRoleMenuIds(id);
        return responseFactory.success(menuIds);
    }

    @PutMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('system:role:assignPermissions')")
    public ApiResponse<Void> assignMenus(@PathVariable Long id,
                                         @RequestBody @Valid AssignMenusRequest request) {
        roleApplicationService.assignMenus(id, request.menuIds());
        return responseFactory.success();
    }
}
