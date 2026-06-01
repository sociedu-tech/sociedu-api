package com.unishare.api.infrastructure.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * UserDetails — authorities chỉ từ role ({@code ROLE_USER}, {@code ROLE_MENTOR}, {@code ROLE_ADMIN}).
 */
@Getter
public class CustomUserPrincipal implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String passwordHash;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;

    public CustomUserPrincipal(UUID userId,
                               String email,
                               String passwordHash,
                               List<String> roles,
                               boolean enabled) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.authorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    /** @deprecated tests legacy — permissions list ignored; use 5-arg constructor. */
    @Deprecated
    public CustomUserPrincipal(UUID userId,
                               String email,
                               String passwordHash,
                               List<String> roles,
                               List<String> ignoredPermissions,
                               boolean enabled) {
        this(userId, email, passwordHash, roles, enabled);
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
