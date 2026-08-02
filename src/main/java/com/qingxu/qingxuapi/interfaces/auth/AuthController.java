package com.qingxu.qingxuapi.interfaces.auth;

import com.qingxu.qingxuapi.application.auth.AuthApplicationService;
import com.qingxu.qingxuapi.application.menu.MenuApplicationService;
import com.qingxu.qingxuapi.common.audit.AuditEventType;
import com.qingxu.qingxuapi.common.audit.AuditService;
import com.qingxu.qingxuapi.common.response.ApiResponse;
import com.qingxu.qingxuapi.common.response.ResponseFactory;
import com.qingxu.qingxuapi.infrastructure.captcha.CaptchaChallenge;
import com.qingxu.qingxuapi.infrastructure.captcha.CaptchaService;
import com.qingxu.qingxuapi.interfaces.auth.dto.CaptchaResponse;
import com.qingxu.qingxuapi.interfaces.auth.dto.CurrentUserResponse;
import com.qingxu.qingxuapi.interfaces.auth.dto.LoginRequest;
import com.qingxu.qingxuapi.interfaces.auth.dto.LoginResponse;
import com.qingxu.qingxuapi.interfaces.auth.dto.MenuTreeResponse;
import com.qingxu.qingxuapi.interfaces.auth.dto.PermissionResponse;
import com.qingxu.qingxuapi.interfaces.auth.dto.RegisterRequest;
import com.qingxu.qingxuapi.interfaces.auth.dto.RegisterResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CaptchaService captchaService;
    private final AuthApplicationService authApplicationService;
    private final MenuApplicationService menuApplicationService;
    private final ResponseFactory responseFactory;
    private final AuditService auditService;

    @PostMapping("/captcha")
    public ApiResponse<CaptchaResponse> captcha() {
        CaptchaChallenge challenge = captchaService.createCaptcha();
        return responseFactory.success(new CaptchaResponse(challenge.captchaKey(), challenge.imageBase64(), challenge.expiresIn()));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.login(request, servletRequest);
        return responseFactory.success(LoginResponse.from(currentUser));
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        AuthApplicationService.RegisterResult result = authApplicationService.register(request, servletRequest);
        return responseFactory.success(new RegisterResponse(result.registered(), result.status(), result.message()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest servletRequest) {
        try {
            CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
            auditService.record(AuditEventType.LOGOUT, true, currentUser.username(), currentUser.id(), servletRequest);
        } catch (Exception e) {
            auditService.record(AuditEventType.LOGOUT, true, null, null, servletRequest);
        }
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return responseFactory.success();
    }

    @GetMapping("/current-user")
    public ApiResponse<CurrentUserResponse> currentUser(HttpServletRequest servletRequest) {
        return responseFactory.success(authApplicationService.currentUser(servletRequest));
    }

    @GetMapping("/routes")
    public ApiResponse<List<MenuTreeResponse>> routes(HttpServletRequest servletRequest) {
        CurrentUserResponse currentUser = authApplicationService.currentUser(servletRequest);
        return responseFactory.success(menuApplicationService.getUserMenus(currentUser.id()));
    }

    @GetMapping("/permissions/current")
    public ApiResponse<PermissionResponse> currentPermissions(HttpServletRequest servletRequest) {
        return responseFactory.success(authApplicationService.currentUserPermissions(servletRequest));
    }
}
