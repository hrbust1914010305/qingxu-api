package com.qingxu.qingxuapi.interfaces.user;

import com.qingxu.qingxuapi.application.auth.AuthApplicationService;
import com.qingxu.qingxuapi.application.user.UserApplicationService;
import com.qingxu.qingxuapi.application.user.UserApplicationService.ResetPasswordResult;
import com.qingxu.qingxuapi.common.response.ApiResponse;
import com.qingxu.qingxuapi.common.response.PageResponse;
import com.qingxu.qingxuapi.common.response.ResponseFactory;
import com.qingxu.qingxuapi.interfaces.auth.dto.CurrentUserResponse;
import com.qingxu.qingxuapi.interfaces.user.dto.*;
import com.qingxu.qingxuapi.interfaces.user.dto.AssignRolesBatchRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserApplicationService userApplicationService;
    private final AuthApplicationService authApplicationService;
    private final ResponseFactory responseFactory;

    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public ApiResponse<PageResponse<UserVO>> list(UserListRequest request, HttpServletRequest servletRequest) {
        authApplicationService.currentUser(servletRequest);
        return responseFactory.success(userApplicationService.list(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:view')")
    public ApiResponse<UserVO> detail(@PathVariable Long id, HttpServletRequest servletRequest) {
        authApplicationService.currentUser(servletRequest);
        return responseFactory.success(userApplicationService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:user:create')")
    public ApiResponse<Long> create(@Valid @RequestBody CreateUserRequest request, HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        Long userId = userApplicationService.create(request, currentUser.id(), currentUser.username(), servletRequest);
        return responseFactory.success(userId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:update')")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request,
                                     HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        userApplicationService.update(id, request, currentUser.id(), currentUser.username(), servletRequest);
        return responseFactory.success(null);
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public ApiResponse<Void> delete(@PathVariable String ids, HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        userApplicationService.delete(ids, currentUser.id(), currentUser.username(), servletRequest);
        return responseFactory.success(null);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:user:update')")
    public ApiResponse<Void> toggleStatus(@PathVariable Long id, @Valid @RequestBody ToggleStatusRequest request,
                                           HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        userApplicationService.toggleStatus(id, request, currentUser.id(), currentUser.username(), servletRequest);
        return responseFactory.success(null);
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('system:user:update')")
    public ApiResponse<ResetPasswordResult> resetPassword(@PathVariable Long id, HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        ResetPasswordResult result = userApplicationService.resetPassword(id, currentUser.id(), currentUser.username(), servletRequest);
        return responseFactory.success(result);
    }

    @PostMapping("/export")
    @PreAuthorize("hasAuthority('system:user:export')")
    public void export(@RequestBody UserExportRequest request, HttpServletResponse response,
                       HttpServletRequest servletRequest) {
        authApplicationService.currentUser(servletRequest);
        userApplicationService.export(request, response);
    }

    @GetMapping("/preference")
    public ApiResponse<UserPreferenceResponse> getUserPreference(HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        UserPreferenceResponse preference = userApplicationService.getUserPreference(currentUser.id());
        return responseFactory.success(preference);
    }

    @PostMapping("/preference/save")
    public ApiResponse<Void> saveUserPreference(
            HttpServletRequest servletRequest,
            @Valid @RequestBody SavePreferenceRequest request) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        userApplicationService.saveUserPreference(currentUser.id(), request);
        return responseFactory.success(null);
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(
            HttpServletRequest servletRequest,
            @Valid @RequestBody UpdateProfileRequest request) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        userApplicationService.updateProfile(currentUser.id(), request);
        return responseFactory.success(null);
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(
            HttpServletRequest servletRequest,
            @Valid @RequestBody ChangePasswordRequest request) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        userApplicationService.changePassword(currentUser.id(), request, currentUser.id(), currentUser.username(), servletRequest);
        return responseFactory.success(null);
    }

    /**
     * 批量为用户分配角色（或清空角色）。
     * 使用权限 {@code system:user:assignRole}。
     */
    @PutMapping("/roles")
    @PreAuthorize("hasAuthority('system:user:assignRole')")
    public ApiResponse<Void> assignRoles(@Valid @RequestBody AssignRolesBatchRequest request,
                                          HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        userApplicationService.assignRolesBatch(request.userIds(),
                request.roleIds(),
                currentUser.id(),
                currentUser.username(),
                servletRequest);
        return responseFactory.success(null);
    }
    }

