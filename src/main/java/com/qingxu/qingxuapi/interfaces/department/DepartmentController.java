package com.qingxu.qingxuapi.interfaces.department;

import com.qingxu.qingxuapi.common.response.ApiResponse;
import com.qingxu.qingxuapi.common.response.ResponseFactory;
import com.qingxu.qingxuapi.application.department.DepartmentApplicationService;
import com.qingxu.qingxuapi.interfaces.department.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/department")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentApplicationService departmentService;
    private final ResponseFactory responseFactory;

    // ========== 部门分类接口 ==========

    @GetMapping("/category/list")
    @PreAuthorize("hasAuthority('department:category:list')")
    public ApiResponse<List<DeptCategoryResponse>> listCategory() {
        List<DeptCategoryResponse> categories = departmentService.listCategory();
        return responseFactory.success(categories);
    }

    @PostMapping("/category")
    @PreAuthorize("hasAuthority('department:category:create')")
    public ApiResponse<Map<String, Long>> createCategory(@Valid @RequestBody CreateDeptCategoryRequest request) {
        Long id = departmentService.createCategory(request);
        return responseFactory.id(id);
    }

    @PutMapping("/category/{id}")
    @PreAuthorize("hasAuthority('department:category:update')")
    public ApiResponse<Void> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeptCategoryRequest request) {
        departmentService.updateCategory(id, request);
        return responseFactory.success();
    }

    @DeleteMapping("/category/{id}")
    @PreAuthorize("hasAuthority('department:category:delete')")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        departmentService.deleteCategory(id);
        return responseFactory.success();
    }

    // ========== 部门接口 ==========

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('department:list')")
    public ApiResponse<List<DepartmentTreeResponse>> getTree(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status
    ) {
        List<DepartmentTreeResponse> tree = departmentService.getTree(name, status);
        return responseFactory.success(tree);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('department:view')")
    public ApiResponse<DepartmentResponse> getDetail(@PathVariable Long id) {
        DepartmentResponse dept = departmentService.getDetail(id);
        return responseFactory.success(dept);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('department:create')")
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody CreateDepartmentRequest request) {
        Long id = departmentService.create(request);
        return responseFactory.id(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('department:update')")
    public ApiResponse<Void> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentRequest request) {
        departmentService.update(id, request);
        return responseFactory.success();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAuthority('department:delete')")
    public ApiResponse<DeleteDepartmentResponse> delete(@PathVariable List<Long> ids) {
        DeleteDepartmentResponse result = departmentService.delete(ids);
        return responseFactory.success(result);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('department:update')")
    public ApiResponse<Void> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentStatusRequest request) {
        departmentService.updateStatus(id, request);
        return responseFactory.success();
    }
}
