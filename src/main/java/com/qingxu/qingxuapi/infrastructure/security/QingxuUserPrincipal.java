package com.qingxu.qingxuapi.infrastructure.security;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

@Getter
@EqualsAndHashCode(of = "id")
@RequiredArgsConstructor
public class QingxuUserPrincipal implements UserDetails {

    private final Long id;
    private final String tenantId;
    private final String username;
    private final String password;
    private final String realname;
    private final String nickname;
    private final String email;
    private final String phone;
    private final String userType;
    private final String status;
    private final Integer failedLoginCount;
    private final java.time.LocalDateTime lockedUntil;
    private final List<String> roles;
    private final List<String> permissions;
    private final List<GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return lockedUntil == null || lockedUntil.isBefore(java.time.LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
