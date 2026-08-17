package org.example.security.entity;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@RequiredArgsConstructor
public class SecurityUserDetails implements UserDetails {

    private final UserSecurity user;

    public Long getId() {
        return user.getId();
    }

    public String getPlatformRole() {
        return user.effectivePlatformRole();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonLocked() {
        return !user.isLockedNow();
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}
