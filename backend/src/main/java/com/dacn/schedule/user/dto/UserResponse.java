package com.dacn.schedule.user.dto;

import com.dacn.schedule.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/** Thông tin tài khoản trả cho client. Không bao giờ chứa mật khẩu / mã băm. */
@Schema(name = "UserResponse", description = "Thông tin tài khoản")
public record UserResponse(
        @Schema(example = "3") Long id,
        @Schema(example = "minhanh@gmail.com") String email,
        @Schema(description = "Họ và tên", example = "Nguyễn Minh Anh") String fullName,
        @Schema(description = "Đường dẫn ảnh đại diện", nullable = true) String avatarUrl,
        @Schema(description = "Email đã được xác thực chưa", example = "false") boolean emailVerified,
        @Schema(description = "Vai trò", example = "[\"ROLE_USER\"]") List<String> roles,
        @Schema(description = "Thời điểm tạo tài khoản", example = "2026-11-03T18:30:00+07:00") OffsetDateTime createdAt) {

    public static UserResponse from(User user, List<String> roles) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getAvatarUrl(),
                user.isEmailVerified(), List.copyOf(roles), user.getCreatedAt());
    }
}
