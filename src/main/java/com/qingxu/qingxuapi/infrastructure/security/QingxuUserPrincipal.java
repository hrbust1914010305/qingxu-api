package com.qingxu.qingxuapi.infrastructure.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

@Getter
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonDeserialize(contentAs = SimpleGrantedAuthority.class)
    private final List<GrantedAuthority> authorities;

    @JsonCreator
    public QingxuUserPrincipal(
            @JsonProperty("id") Long id,
            @JsonProperty("tenantId") String tenantId,
            @JsonProperty("username") String username,
            @JsonProperty("password") String password,
            @JsonProperty("realname") String realname,
            @JsonProperty("nickname") String nickname,
            @JsonProperty("email") String email,
            @JsonProperty("phone") String phone,
            @JsonProperty("userType") String userType,
            @JsonProperty("status") String status,
            @JsonProperty("failedLoginCount") Integer failedLoginCount,
            @JsonProperty("lockedUntil") java.time.LocalDateTime lockedUntil,
            @JsonProperty("roles") List<String> roles,
            @JsonProperty("permissions") List<String> permissions,
            @JsonProperty("authorities") @JsonDeserialize(contentAs = SimpleGrantedAuthority.class) List<GrantedAuthority> authorities
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.username = username;
        this.password = password;
        this.realname = realname;
        this.nickname = nickname;
        this.email = email;
        this.phone = phone;
        this.userType = userType;
        this.status = status;
        this.failedLoginCount = failedLoginCount;
        this.lockedUntil = lockedUntil;
        this.roles = roles;
        this.permissions = permissions;
        this.authorities = authorities;
    }

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
