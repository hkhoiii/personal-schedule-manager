package com.dacn.schedule.auth.security;

import java.time.Instant;
import java.util.List;

/**
 * Thông tin đọc được từ một access token đã kiểm tra hợp lệ (chữ ký, hạn dùng, issuer).
 *
 * @param userId    id người dùng (claim {@code sub})
 * @param email     email lúc cấp token
 * @param roles     vai trò lúc cấp token, ví dụ {@code ROLE_USER}
 * @param expiresAt thời điểm hết hạn (claim {@code exp})
 */
public record AccessTokenClaims(Long userId, String email, List<String> roles, Instant expiresAt) {

    public AccessTokenClaims {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
