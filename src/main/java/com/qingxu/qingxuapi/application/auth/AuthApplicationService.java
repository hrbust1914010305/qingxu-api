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
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
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

        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(SESSION_CURRENT_USER, currentUserResponse);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());

        sessionRegistry.registerNewSession(session.getId(), principal);

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
        HttpSession session = servletRequest.getSession(false);
        if (session == null) {
            throw new UnauthorizedException();
        }
        Object value = session.getAttribute(SESSION_CURRENT_USER);
        if (!(value instanceof CurrentUserResponse currentUserResponse)) {
            throw new UnauthorizedException();
        }
        return currentUserResponse;
    }

    public PermissionResponse currentUserPermissions(HttpServletRequest servletRequest) {
        CurrentUserResponse user = currentUser(servletRequest);

        List<String> allPermissions = new ArrayList<>(user.permissions());
        allPermissions.addAll(DEPT_PERMISSION_CODES);

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