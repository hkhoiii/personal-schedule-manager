package com.dacn.schedule.auth.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cấu hình JWT, đọc từ {@code app.jwt.*} trong application.properties.
 *
 * @param secret         khóa ký HS256 dạng Base64 (biến môi trường {@code JWT_SECRET}); kiểm tra chi tiết
 *                       (Base64 hợp lệ, đủ 32 byte) nằm ở {@link JwtService} để báo lỗi dễ hiểu hơn
 * @param accessTokenTtl  thời gian sống của access token (JWT)
 * @param refreshTokenTtl thời gian sống của refresh token (chuỗi ngẫu nhiên lưu ở bảng tokenlammoi)
 * @param issuer          giá trị claim {@code iss}
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        @NotNull Duration accessTokenTtl,
        @NotNull Duration refreshTokenTtl,
        @NotBlank String issuer) {

    /** Không in khóa bí mật ra log nếu lỡ log cả đối tượng cấu hình. */
    @Override
    public String toString() {
        return "JwtProperties[secret=***, accessTokenTtl=" + accessTokenTtl + ", refreshTokenTtl=" + refreshTokenTtl
                + ", issuer=" + issuer + "]";
    }
}
