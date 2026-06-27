package com.qingxu.qingxuapi.interfaces.user;

import com.qingxu.qingxuapi.application.auth.AuthApplicationService;
import com.qingxu.qingxuapi.application.user.UserApplicationService;
import com.qingxu.qingxuapi.common.response.ApiResponse;
import com.qingxu.qingxuapi.common.response.ResponseFactory;
import com.qingxu.qingxuapi.interfaces.auth.dto.CurrentUserResponse;
import com.qingxu.qingxuapi.interfaces.user.dto.ChangePasswordRequest;
import com.qingxu.qingxuapi.interfaces.user.dto.SavePreferenceRequest;
import com.qingxu.qingxuapi.interfaces.user.dto.UpdateProfileRequest;
import com.qingxu.qingxuapi.interfaces.user.dto.UserPreferenceResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserApplicationService userApplicationService;
    private final AuthApplicationService authApplicationService;
    private final ResponseFactory responseFactory;

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
        userApplicationService.changePassword(currentUser.id(), request);
        return responseFactory.success(null);
    }
}