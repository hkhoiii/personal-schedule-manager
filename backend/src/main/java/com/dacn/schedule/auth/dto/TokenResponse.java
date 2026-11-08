package com.dacn.schedule.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Cặp token trả về khi đăng nhập / làm mới.
 *
 * <p>Client gửi access token trong header {@code Authorization: Bearer <accessToken>} cho mọi API; khi nhận lỗi
 * 401 {@code TOKEN_EXPIRED} thì gọi {@code POST /auth/refresh} với refresh token để lấy cặp mới. Refresh token cũ
 * bị thu hồi ngay sau mỗi lần làm mới, nên client phải lưu refresh token MỚI.
 */
@Schema(name = "TokenResponse", description = "Access token (JWT) và refresh token")
public record TokenResponse(
        @Schema(description = "JWT, gửi trong header Authorization: Bearer ...", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(description = "Luôn là Bearer", example = "Bearer")
        String tokenType,
        @Schema(description = "Số giây access token còn hiệu lực", example = "900")
        long expiresIn,
        @Schema(description = "Dùng một lần để gọi /auth/refresh", example = "q3J8s0d2W5mZ1yT7kL9xV4cB6nH0fR2aE8uI3oP5sD1")
        String refreshToken,
        @Schema(description = "Số giây refresh token còn hiệu lực", example = "604800")
        long refreshExpiresIn) {

    @Override
    public String toString() {
        return "TokenResponse[tokenType=" + tokenType + ", expiresIn=" + expiresIn + ", tokens=***]";
    }
}
