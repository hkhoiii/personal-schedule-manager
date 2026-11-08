package com.dacn.schedule.auth.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Tài khoản dưới dạng Spring Security hiểu được, chỉ dùng lúc đăng nhập (kiểm tra mật khẩu).
 * Giữ thêm {@code userId} để cấp token sau khi đăng nhập thành công.
 */
public class AccountUserDetails implements UserDetails {

    private final Long userId;
    private final String email;
    private final String passwordHash;
    private final boolean enabled;
    private final List<String> roles;

    public AccountUserDetails(Long userId, String email, String passwordHash, boolean enabled, List<String> roles) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.roles = List.copyOf(roles);
    }

    public Long getUserId() {
        return userId;
    }

    public List<String> getRoles() {
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(SimpleGrantedAuthority::new).toList();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
