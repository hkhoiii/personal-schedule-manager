package com.dacn.schedule.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dacn.schedule.auth.dto.LoginRequest;
import com.dacn.schedule.auth.dto.RegisterRequest;
import com.dacn.schedule.auth.dto.TokenResponse;
import com.dacn.schedule.auth.entity.RefreshToken;
import com.dacn.schedule.auth.repository.RefreshTokenRepository;
import com.dacn.schedule.auth.security.AccessTokenClaims;
import com.dacn.schedule.auth.security.JwtService;
import com.dacn.schedule.common.exception.ApiException;
import com.dacn.schedule.common.exception.ErrorCode;
import com.dacn.schedule.user.dto.UserResponse;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đăng nhập / làm mới / đăng xuất với PostgreSQL thật (bảng nguoidung, vaitronguoidung, tokenlammoi).
 * Mỗi test chạy trong một transaction và được rollback, không để lại dữ liệu.
 */
@SpringBootTest
@Transactional
class AuthTokenIntegrationTest {

    private static final String EMAIL = "token.test@gmail.com";
    private static final String PASSWORD = "matkhau123";

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private UserResponse user;

    @BeforeEach
    void registerUser() {
        user = authService.register(new RegisterRequest(EMAIL, PASSWORD, "Token Test"));
    }

    @Test
    void login_returnsValidAccessTokenAndStoresOnlyHashOfRefreshToken() {
        TokenResponse tokens = authService.login(new LoginRequest(" Token.Test@Gmail.com ", PASSWORD), "JUnit");

        AccessTokenClaims claims = jwtService.parseAccessToken(tokens.accessToken());
        assertThat(claims.userId()).isEqualTo(user.id());
        assertThat(claims.email()).isEqualTo(EMAIL);
        assertThat(claims.roles()).containsExactly("ROLE_USER");
        assertThat(tokens.tokenType()).isEqualTo("Bearer");
        assertThat(tokens.expiresIn()).isEqualTo(900);
        assertThat(tokens.refreshExpiresIn()).isEqualTo(7 * 24 * 3600);

        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(RefreshTokenService.sha256Hex(tokens.refreshToken())).orElseThrow();
        assertThat(stored.getUserId()).isEqualTo(user.id());
        assertThat(stored.getTokenHash()).hasSize(64).isNotEqualTo(tokens.refreshToken());
        assertThat(stored.getRevokedAt()).isNull();
        assertThat(stored.getDevice()).isEqualTo("JUnit");
    }

    @Test
    void login_wrongPasswordOrUnknownEmail_throwsBadCredentials() {
        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "saimatkhau1"), null))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> authService.login(new LoginRequest("khong.ton.tai@gmail.com", PASSWORD), null))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_passwordOver72Bytes_throwsBadCredentialsInsteadOfServerError() {
        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "ữ".repeat(30) + "1"), null))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_issuesNewPairAndRevokesOldRefreshToken() {
        TokenResponse first = authService.login(new LoginRequest(EMAIL, PASSWORD), null);

        TokenResponse second = authService.refresh(first.refreshToken(), null);

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(jwtService.parseAccessToken(second.accessToken()).userId()).isEqualTo(user.id());
        assertThat(findStored(first.refreshToken()).getRevokedAt()).isNotNull();
        assertThat(findStored(second.refreshToken()).getRevokedAt()).isNull();
    }

    @Test
    void refresh_reusingRotatedToken_revokesAllSessionsOfUser() {
        TokenResponse first = authService.login(new LoginRequest(EMAIL, PASSWORD), null);
        TokenResponse second = authService.refresh(first.refreshToken(), null);

        // Token cũ bị dùng lại => có thể đã bị đánh cắp => thu hồi luôn token mới nhất
        assertErrorCode(() -> authService.refresh(first.refreshToken(), null), ErrorCode.TOKEN_EXPIRED);
        assertErrorCode(() -> authService.refresh(second.refreshToken(), null), ErrorCode.TOKEN_EXPIRED);
    }

    @Test
    void refresh_unknownToken_throwsTokenExpired() {
        assertErrorCode(() -> authService.refresh("token-khong-ton-tai", null), ErrorCode.TOKEN_EXPIRED);
    }

    @Test
    void logout_revokesRefreshToken_andIsIdempotent() {
        TokenResponse tokens = authService.login(new LoginRequest(EMAIL, PASSWORD), null);

        authService.logout(tokens.refreshToken());
        authService.logout(tokens.refreshToken());
        authService.logout("token-khong-ton-tai");

        assertThat(findStored(tokens.refreshToken()).getRevokedAt()).isNotNull();
        assertErrorCode(() -> authService.refresh(tokens.refreshToken(), null), ErrorCode.TOKEN_EXPIRED);
    }

    private RefreshToken findStored(String rawToken) {
        return refreshTokenRepository.findByTokenHash(RefreshTokenService.sha256Hex(rawToken)).orElseThrow();
    }

    private static void assertErrorCode(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(code);
    }
}
