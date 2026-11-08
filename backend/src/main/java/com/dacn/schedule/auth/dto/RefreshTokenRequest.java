package com.dacn.schedule.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Thân yêu cầu của /auth/refresh và /auth/logout. */
@Schema(name = "RefreshTokenRequest", description = "Refresh token nhận được khi đăng nhập / làm mới")
public record RefreshTokenRequest(

        @Schema(example = "q3J8s0d2W5mZ1yT7kL9xV4cB6nH0fR2aE8uI3oP5sD1")
        @NotBlank(message = "Refresh token không được để trống")
        @Size(max = 200, message = "Refresh token không hợp lệ")
        String refreshToken) {

    @Override
    public String toString() {
        return "RefreshTokenRequest[refreshToken=***]";
    }
}
