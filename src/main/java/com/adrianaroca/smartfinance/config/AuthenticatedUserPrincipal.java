package com.adrianaroca.smartfinance.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public final class AuthenticatedUserPrincipal implements UserDetails, CredentialsContainer {
    private final Long userId;
    private final String email;
    private String password;
    private final boolean enabled;
    private final long credentialVersion;

    public AuthenticatedUserPrincipal(Long userId, String email, String password,
                                      boolean enabled, long credentialVersion) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.enabled = enabled;
        this.credentialVersion = credentialVersion;
    }

    public Long getUserId() { return userId; }
    public long getCredentialVersion() { return credentialVersion; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
    @Override public String getPassword() { return password; }
    @Override public void eraseCredentials() { password = null; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}
