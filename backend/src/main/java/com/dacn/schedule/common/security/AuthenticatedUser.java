package com.dacn.schedule.common.security;

import java.util.List;

/**
 * Người dùng đang đăng nhập, lấy từ access token hợp lệ. Bộ lọc JWT đặt đối tượng này vào SecurityContext.
 *
 * <p>Controller của mọi module lấy người dùng hiện tại bằng:
 * <pre>{@code
 * @GetMapping("/me")
 * public UserResponse me(@AuthenticationPrincipal AuthenticatedUser currentUser) { ... currentUser.userId() ... }
 * }</pre>
 * Đặt ở {@code common} để module khác dùng được mà không phụ thuộc vào module auth.
 *
 * @param userId id người dùng (bảng nguoidung)
 * @param email  email lúc cấp token
 * @param roles  vai trò lúc cấp token, ví dụ {@code ROLE_USER}
 */
public record AuthenticatedUser(Long userId, String email, List<String> roles) {

    public AuthenticatedUser {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
