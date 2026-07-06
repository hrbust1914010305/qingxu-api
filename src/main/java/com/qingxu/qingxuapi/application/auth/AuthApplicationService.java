package com.qingxu.qingxuapi.application.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingxu.qingxuapi.common.audit.AuditEventType;
import com.qingxu.qingxuapi.common.audit.AuditService;
import com.qingxu.qingxuapi.common.exception.BusinessException;
import com.qingxu.qingxuapi.common.exception.UnauthorizedException;
import com.qingxu.qingxuapi.common.response.ErrorCode;
import com.qingxu.qingxuapi.infrastructure.captcha.CaptchaService;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysPermissionEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysRoleEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysPermissionMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserMapper;
import com.qingxu.qingxuapi.infrastructure.security.QingxuUserPrincipal;
import com.qingxu.qingxuapi.interfaces.auth.dto.CurrentUserResponse;
import com.qingxu.qingxuapi.interfaces.auth.dto.LoginRequest;
import jakarta.servlet.http.HttpSession;
import com.qingxu.qingxuapi.interfaces.auth.dto.PermissionResponse;
import com.qingxu.qingxuapi.interfaces.auth.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.session.FindByIndexNameSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthApplicationService {

    public static final String SESSION_CURRENT_USER = "SESSION_CURRENT_USER";
    private static final String DEFAULT_TENANT_ID = "default";
    private static final String DEFAULT_USER_TYPE = "EXTERNAL";
    private static final String REGISTER_PENDING_MESSAGE = "注册已提交，请等待管理员审核";

    private static final List<String> DEPT_PERMISSION_CODES = List.of(
            "department:list",
            "department:view",
            "department:create",
            "department:update",
            "department:delete",
            "department:category:list",
            "department:category:create",
            "department:category:update",
            "department:category:delete"
    );

    private static final List<GrantedAuthority> DEPT_AUTHORITIES = DEPT_PERMISSION_CODES.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

    private static final List<String> MENU_PERMISSION_CODES = List.of(
            "system:menu:list",
            "system:menu:create",
            "system:menu:update",
            "system:menu:delete"
    );

    private static final List<GrantedAuthority> MENU_AUTHORITIES = MENU_PERMISSION_CODES.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

    private final CaptchaService captchaService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SessionRegistry sessionRegistry;
    private final com.qingxu.qingxuapi.application.user.UserApplicationService userApplicationService;

    public CurrentUserResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        String captchaKey = request.captchaKey();
        String captcha = request.captcha();
        captchaService.assertCaptcha(captchaKey, captcha);

        SysUserEntity user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, request.username())
                .eq(SysUserEntity::getDeleted, 0));

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            if (user != null) {
                handleFailedLogin(user, servletRequest);
            }
            captchaService.invalidateCaptcha(captchaKey);
            auditService.record(AuditEventType.LOGIN_FAILURE, false, request.username(), user == null ? null : user.getId(), servletRequest);
            throw new BusinessException(ErrorCode.AUTH_BAD_CREDENTIALS);
        }

        if (isUserLocked(user)) {
            auditService.record(AuditEventType.LOGIN_FAILURE, false, request.username(), user.getId(), servletRequest);
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            captchaService.invalidateCaptcha(captchaKey);
            auditService.record(AuditEventType.LOGIN_FAILURE, false, request.username(), user.getId(), servletRequest);
            throw new BusinessException(ErrorCode.AUTH_BAD_CREDENTIALS);
        }

        resetLoginState(user, servletRequest);

        List<SysRoleEntity> roles = roleMapper.selectByUserId(user.getId());
        List<SysPermissionEntity> permissions = permissionMapper.selectByUserId(user.getId());
        List<String> roleNames = roles.stream().map(SysRoleEntity::getCode).collect(Collectors.toList());
        List<String> permissionCodes = permissions.stream().map(SysPermissionEntity::getCode).collect(Collectors.toList());
        List<GrantedAuthority> authorities = new ArrayList<>(permissionCodes.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()));
        authorities.addAll(DEPT_AUTHORITIES);
        authorities.addAll(MENU_AUTHORITIES);

        CurrentUserResponse currentUserResponse = toCurrentUserResponse(user, roleNames, permissionCodes);

        QingxuUserPrincipal principal = new QingxuUserPrincipal(
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getRealname(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.getUserType(),
                user.getStatus(),
                user.getFailedLoginCount(),
                user.getLockedUntil(),
                roleNames,
                permissionCodes,
                authorities
        );

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("=== [login] 开始创建 Session ===");
        log.debug("SecurityContext 已设置: {}", SecurityContextHolder.getContext().getAuthentication());

        HttpSession session = servletRequest.getSession(true);
        log.debug("✅ [login] Session 创建成功: ID={}", session.getId());
        log.debug("[login] Session isNew: {}", session.isNew());

        session.setAttribute(SESSION_CURRENT_USER, currentUserResponse);
        log.debug("✅ [login] SESSION_CURRENT_USER 已存入 Session");
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );
        log.debug("✅ [login] SPRING_SECURITY_CONTEXT 已存入 Session");

        log.debug("[login] 存入的用户信息: id={}, username={}, roles={}",
                currentUserResponse.id(),
                currentUserResponse.username(),
                currentUserResponse.roles());

        session.setAttribute(
                FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
                String.valueOf(user.getId())
        );
        log.debug("✅ [login] PRINCIPAL_NAME_INDEX_NAME 已设置: userId={}", user.getId());

        sessionRegistry.registerNewSession(session.getId(), principal);
        log.debug("✅ [login] Session 已注册到 SessionRegistry");

        log.debug("=== [login] 登录完成，准备返回用户信息 ===");

        auditService.record(AuditEventType.LOGIN_SUCCESS, true, request.username(), user.getId(), servletRequest);
        return currentUserResponse;
    }

    public RegisterResult register(RegisterRequest request, HttpServletRequest servletRequest) {
        captchaService.assertCaptcha(request.captchaKey(), request.captcha());
        validatePasswordStrength(request.password());
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "两次输入的密码不一致");
        }

        SysUserEntity existingUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, request.username())
                .eq(SysUserEntity::getDeleted, 0));
        if (existingUser != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "用户名已被注册，请使用其他用户名");
        }
        if (request.email() != null && !request.email().isBlank()) {
            SysUserEntity emailUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                    .eq(SysUserEntity::getEmail, request.email())
                    .eq(SysUserEntity::getDeleted, 0));
            if (emailUser != null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "该邮箱已被注册，请使用其他邮箱");
            }
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            SysUserEntity phoneUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                    .eq(SysUserEntity::getPhone, request.phone())
                    .eq(SysUserEntity::getDeleted, 0));
            if (phoneUser != null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "该手机号已被注册，请使用其他手机号");
            }
        }

        SysUserEntity user = new SysUserEntity();
        user.setTenantId(DEFAULT_TENANT_ID);
        user.setUsername(request.username());
        user.setRealname(null);
        user.setNickname(request.nickname());
        user.setAvatar(null);
        user.setEmail((request.email() != null && !request.email().isBlank()) ? request.email() : null);
        user.setPhone((request.phone() != null && !request.phone().isBlank()) ? request.phone() : null);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUserType(DEFAULT_USER_TYPE);
        user.setStatus("PENDING");
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(null);
        user.setLastLoginIp(null);
        user.setCreatedBy(null);
        user.setUpdatedBy(null);
        user.setDeleted(0);
        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "用户已存在");
        }
        userApplicationService.joinRootTempDept(user.getId());
        auditService.record(AuditEventType.REGISTER_SUCCESS, true, request.username(), null, servletRequest);
        return new RegisterResult(true, "PENDING", REGISTER_PENDING_MESSAGE);
    }

    public CurrentUserResponse currentUser(HttpServletRequest servletRequest) {
        log.debug("=== [currentUser] 开始获取当前用户 ===");
        log.debug("Request URI: {}", servletRequest.getRequestURI());
        log.debug("Session ID from Cookie: {}", servletRequest.getRequestedSessionId());
        log.debug("Is Requested Session Valid: {}", servletRequest.isRequestedSessionIdValid());

        HttpSession session = servletRequest.getSession(false);
        log.debug("HttpSession object: {}", session);

        if (session == null) {
            log.error("❌ [currentUser] Session 为 null！可能原因：");
            log.error("   1. 未登录");
            log.error("   2. Session 已过期");
            log.error("   3. Cookie 未正确传递（检查 withCredentials 配置）");
            log.error("   4. SameSite 策略阻止了 Cookie 发送");
            throw new UnauthorizedException();
        }

        log.debug("✅ Session ID: {}", session.getId());
        log.debug("Session Creation Time: {}", session.getCreationTime());
        log.debug("Session Last Accessed: {}", session.getLastAccessedTime());
        log.debug("Session Max Inactive Interval: {} seconds", session.getMaxInactiveInterval());

        log.debug("Session Attributes:");
        java.util.Enumeration<String> attrNames = session.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String attrName = attrNames.nextElement();
            Object attrValue = session.getAttribute(attrName);
            log.debug("  - {}: {}", attrName, attrValue != null ? attrValue.getClass().getSimpleName() : "null");
        }

        Object value = session.getAttribute(SESSION_CURRENT_USER);
        log.debug("SESSION_CURRENT_USER value: {}", value);
        log.debug("SESSION_CURRENT_USER type: {}", value != null ? value.getClass().getName() : "null");

        if (!(value instanceof CurrentUserResponse currentUserResponse)) {
            log.error("❌ [currentUser] SESSION_CURRENT_USER 类型不匹配或为空！");
            log.error("   实际类型: {}", value != null ? value.getClass().getName() : "null");
            log.error("   期望类型: CurrentUserResponse");
            throw new UnauthorizedException();
        }

        log.debug("✅ [currentUser] 成功获取用户信息: id={}, username={}", currentUserResponse.id(), currentUserResponse.username());
        return currentUserResponse;
    }

    public PermissionResponse currentUserPermissions(HttpServletRequest servletRequest) {
        CurrentUserResponse user = currentUser(servletRequest);

        List<String> allPermissions = new ArrayList<>(user.permissions());
        allPermissions.addAll(DEPT_PERMISSION_CODES);
        allPermissions.addAll(MENU_PERMISSION_CODES);

        return new PermissionResponse(user.roles(), allPermissions);
    }

    private CurrentUserResponse toCurrentUserResponse(SysUserEntity user, List<String> roles, List<String> permissions) {
        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRealname(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.getTenantId(),
                user.getUserType(),
                user.getStatus(),
                user.getNeedPasswordChange(),
                roles,
                permissions
        );
    }

    private void handleFailedLogin(SysUserEntity user, HttpServletRequest servletRequest) {
        Integer failedCount = user.getFailedLoginCount() == null ? 0 : user.getFailedLoginCount();
        int updatedCount = failedCount + 1;
        user.setFailedLoginCount(updatedCount);
        if (updatedCount >= 5) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        }
        sysUserMapper.updateById(user);
        auditService.record(AuditEventType.LOGIN_FAILURE, false, user.getUsername(), user.getId(), servletRequest);
    }

    private void resetLoginState(SysUserEntity user, HttpServletRequest servletRequest) {
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(servletRequest.getRemoteAddr());
        sysUserMapper.updateById(user);
    }

    private boolean isUserLocked(SysUserEntity user) {
        if (user.getLockedUntil() == null) {
            return false;
        }
        return user.getLockedUntil().isAfter(LocalDateTime.now());
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "密码长度不能少于8位");
        }
        boolean hasUpperCase = !password.equals(password.toLowerCase());
        boolean hasLowerCase = !password.equals(password.toUpperCase());
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

        if (!hasUpperCase || !hasLowerCase || !hasDigit || !hasSpecial) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "密码必须包含大写字母、小写字母、数字和特殊字符（如!@#$%等）");
        }
    }

    public record RegisterResult(boolean registered, String status, String message) {
    }
}
