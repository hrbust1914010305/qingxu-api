package com.qingxu.qingxuapi.interfaces.department;

import com.qingxu.qingxuapi.application.department.DepartmentApplicationService;
import com.qingxu.qingxuapi.common.response.ApiResponse;
import com.qingxu.qingxuapi.common.response.ResponseFactory;
import com.qingxu.qingxuapi.interfaces.department.dto.CreateDepartmentRequest;
import com.qingxu.qingxuapi.interfaces.department.dto.CreateDeptCategoryRequest;
import com.qingxu.qingxuapi.interfaces.department.dto.DeleteDepartmentResponse;
import com.qingxu.qingxuapi.interfaces.department.dto.DepartmentResponse;
import com.qingxu.qingxuapi.interfaces.department.dto.DepartmentTreeResponse;
import com.qingxu.qingxuapi.interfaces.department.dto.DeptCategoryResponse;
import com.qingxu.qingxuapi.interfaces.department.dto.UpdateDepartmentRequest;
import com.qingxu.qingxuapi.interfaces.department.dto.UpdateDepartmentStatusRequest;
import com.qingxu.qingxuapi.interfaces.department.dto.UpdateDeptCategoryRequest;
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
@RequestMapping("/api/department")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentApplicationService departmentService;
    private final ResponseFactory responseFactory;

    @GetMapping("/category/list")
    @PreAuthorize("hasAuthority('system:department:list')")
    public ApiResponse<List<DeptCategoryResponse>> listCategory() {
        return responseFactory.success(departmentService.listCategory());
    }

    @PostMapping("/category")
    @PreAuthorize("hasAuthority('system:department:create')")
    public ApiResponse<Map<String, Long>> createCategory(@Valid @RequestBody CreateDeptCategoryRequest request) {
        return responseFactory.id(departmentService.createCategory(request));
    }

    @PutMapping("/category/{id}")
    @PreAuthorize("hasAuthority('system:department:update')")
    public ApiResponse<Void> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeptCategoryRequest request
    ) {
        departmentService.updateCategory(id, request);
        return responseFactory.success();
    }

    @DeleteMapping("/category/{id}")
    @PreAuthorize("hasAuthority('system:department:delete')")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        departmentService.deleteCategory(id);
        return responseFactory.success();
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:department:list')")
    public ApiResponse<List<DepartmentTreeResponse>> getTree(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status
    ) {
        return responseFactory.success(departmentService.getTree(name, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:department:view')")
    public ApiResponse<DepartmentResponse> getDetail(@PathVariable Long id) {
        return responseFactory.success(departmentService.getDetail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:department:create')")
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody CreateDepartmentRequest request) {
        return responseFactory.id(departmentService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:department:update')")
    public ApiResponse<Void> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentRequest request
    ) {
        departmentService.update(id, request);
        return responseFactory.success();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAuthority('system:department:delete')")
    public ApiResponse<DeleteDepartmentResponse> delete(@PathVariable List<Long> ids) {
        return responseFactory.success(departmentService.delete(ids));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:department:update')")
    public ApiResponse<Void> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentStatusRequest request
    ) {
        departmentService.updateStatus(id, request);
        return responseFactory.success();
    }
}
