package com.qingxu.qingxuapi.infrastructure.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysPermissionEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysRoleEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.entity.SysUserEntity;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysPermissionMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysRoleMapper;
import com.qingxu.qingxuapi.infrastructure.persistence.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QingxuUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUserEntity user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, username)
                .eq(SysUserEntity::getDeleted, 0));
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        List<String> roles = loadRoleCodes(user);
        List<String> permissions = loadPermissionCodes(user);
        List<GrantedAuthority> authorities = new ArrayList<>();
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        return new QingxuUserPrincipal(
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
                roles,
                permissions,
                authorities
        );
    }

    private List<String> loadRoleCodes(SysUserEntity user) {
        if (user == null || user.getId() == null) {
            return List.of();
        }
        return sysRoleMapper.selectByUserId(user.getId())
                .stream()
                .map(SysRoleEntity::getCode)
                .filter(code -> code != null && !code.isBlank())
                .toList();
    }

    private List<String> loadPermissionCodes(SysUserEntity user) {
        if (user == null || user.getId() == null) {
            return List.of();
        }
        return sysPermissionMapper.selectByUserId(user.getId())
                .stream()
                .map(SysPermissionEntity::getCode)
                .filter(code -> code != null && !code.isBlank())
                .toList();
    }
}
