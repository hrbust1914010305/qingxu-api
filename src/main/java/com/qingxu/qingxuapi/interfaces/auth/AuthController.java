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
@RequestMapping("/api/auth")
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
    public ApiResponse<CurrentUserResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        System.out.println("========================================");
        System.out.println("[AuthController] 收到登录请求");
        System.out.println("  - Username: " + request.username());
        System.out.println("  - Request URI: " + servletRequest.getRequestURI());
        System.out.println("  - Cookies: ");
        if (servletRequest.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : servletRequest.getCookies()) {
                System.out.println("    * " + cookie.getName() + " = " + (cookie.getName().contains("SESSION") ? "[HIDDEN]" : cookie.getValue()));
            }
        } else {
            System.out.println("    (无 Cookie)");
        }
        System.out.println("========================================");

        ApiResponse<CurrentUserResponse> response = responseFactory.success(authApplicationService.login(request, servletRequest));

        System.out.println("========================================");
        System.out.println("[AuthController] 登录成功，返回响应");
        System.out.println("========================================");

        return response;
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
        return responseFactory.success(null);
    }

    @GetMapping("/current-user")
    public ApiResponse<CurrentUserResponse> currentUser(HttpServletRequest servletRequest) {
        System.out.println("========================================");
        System.out.println("[AuthController] 收到 /current-user 请求");
        System.out.println("  - Request URI: " + servletRequest.getRequestURI());
        System.out.println("  - Request Method: " + servletRequest.getMethod());
        System.out.println("  - Session ID (from cookie): " + servletRequest.getRequestedSessionId());
        System.out.println("  - Is Session Valid: " + servletRequest.isRequestedSessionIdValid());
        System.out.println("  - Cookies: ");
        if (servletRequest.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : servletRequest.getCookies()) {
                System.out.println("    * " + cookie.getName() +
                        " = " + (cookie.getName().contains("SESSION") ? "[VALUE_LENGTH=" + cookie.getValue().length() + "]" : cookie.getValue()) +
                        " [Domain=" + cookie.getDomain() +
                        ", Path=" + cookie.getPath() +
                        ", MaxAge=" + cookie.getMaxAge() +
                        ", HttpOnly=" + cookie.isHttpOnly() +
                        ", Secure=" + cookie.getSecure() +
                        "]");
            }
        } else {
            System.out.println("    ⚠️  (无 Cookie - 这就是401的原因!)");
        }
        System.out.println("  - Authorization Header: " + servletRequest.getHeader("Authorization"));
        System.out.println("========================================");

        try {
            CurrentUserResponse user = authApplicationService.currentUser(servletRequest);
            System.out.println("✅ [AuthController] /current-user 成功: username=" + user.username());
            return responseFactory.success(user);
        } catch (Exception e) {
            System.err.println("❌ [AuthController] /current-user 失败: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            throw e;
        }
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