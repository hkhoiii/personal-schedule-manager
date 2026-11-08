package com.dacn.schedule.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dacn.schedule.common.exception.ApiException;
import com.dacn.schedule.common.exception.ErrorCode;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Kiểm tra {@link JwtService} không cần Spring / database. Thời gian do {@link MutableClock} điều khiển. */
class JwtServiceTest {

    private static final String SECRET = base64("0123456789abcdef0123456789abcdef0123456789abcdef"); // 48 byte
    private static final String OTHER_SECRET = base64("ffffffffffffffffffffffffffffffffffffffffffffffff");
    private static final Instant NOW = Instant.parse("2026-11-04T01:00:00Z");

    private final MutableClock clock = new MutableClock(NOW);
    private final JwtService jwtService = newService(SECRET, "schedule", clock);

    @Test
    void generateThenParse_returnsSameUserEmailRolesAndExpiry() {
        String token = jwtService.generateAccessToken(7L, "an@gmail.com", List.of("ROLE_USER"));

        AccessTokenClaims claims = jwtService.parseAccessToken(token);

        assertThat(token.split("\\.")).hasSize(3);
        assertThat(claims.userId()).isEqualTo(7L);
        assertThat(claims.email()).isEqualTo("an@gmail.com");
        assertThat(claims.roles()).containsExactly("ROLE_USER");
        assertThat(claims.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
    }

    @Test
    void tokenIsStillValidJustBeforeExpiry() {
        String token = jwtService.generateAccessToken(7L, "an@gmail.com", List.of("ROLE_USER"));
        clock.advance(Duration.ofMinutes(14));

        assertThat(jwtService.parseAccessToken(token).userId()).isEqualTo(7L);
    }

    @Test
    void expiredToken_throwsTokenExpired() {
        String token = jwtService.generateAccessToken(7L, "an@gmail.com", List.of("ROLE_USER"));
        clock.advance(Duration.ofMinutes(16));

        assertErrorCode(token, ErrorCode.TOKEN_EXPIRED);
    }

    @Test
    void tamperedPayload_throwsUnauthenticated() {
        String token = jwtService.generateAccessToken(7L, "an@gmail.com", List.of("ROLE_USER"));
        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"1\",\"roles\":[\"ROLE_ADMIN\"],\"iss\":\"schedule\",\"exp\":9999999999}"
                        .getBytes(StandardCharsets.UTF_8));

        assertErrorCode(parts[0] + "." + forgedPayload + "." + parts[2], ErrorCode.UNAUTHENTICATED);
    }

    @Test
    void tokenSignedWithAnotherSecret_throwsUnauthenticated() {
        String foreignToken = newService(OTHER_SECRET, "schedule", clock)
                .generateAccessToken(7L, "an@gmail.com", List.of("ROLE_USER"));

        assertErrorCode(foreignToken, ErrorCode.UNAUTHENTICATED);
    }

    @Test
    void tokenFromAnotherIssuer_throwsUnauthenticated() {
        String foreignToken = newService(SECRET, "ung-dung-khac", clock)
                .generateAccessToken(7L, "an@gmail.com", List.of("ROLE_USER"));

        assertErrorCode(foreignToken, ErrorCode.UNAUTHENTICATED);
    }

    @Test
    void unsignedToken_throwsUnauthenticated() {
        // Token "alg":"none" - kiểu tấn công cổ điển: bỏ chữ ký và hy vọng server vẫn chấp nhận
        String unsigned = Jwts.builder().subject("1").issuer("schedule")
                .expiration(Date.from(NOW.plusSeconds(600))).compact();

        assertErrorCode(unsigned, ErrorCode.UNAUTHENTICATED);
    }

    @Test
    void garbageOrEmptyToken_throwsUnauthenticated() {
        assertErrorCode("khong-phai-token", ErrorCode.UNAUTHENTICATED);
        assertErrorCode("", ErrorCode.UNAUTHENTICATED);
    }

    @Test
    void missingSecret_failsAtStartupWithClearMessage() {
        assertThatThrownBy(() -> newService("  ", "schedule", clock))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Chưa cấu hình JWT_SECRET");
    }

    @Test
    void placeholderSecretFromEnvExample_isRejectedAsNotBase64() {
        assertThatThrownBy(() -> newService("change_me_run_openssl_rand_base64_48", "schedule", clock))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
    }

    @Test
    void shortSecret_isRejectedWithoutLeakingIt() {
        String shortSecret = base64("chi-co-16-byte!!"); // 16 byte < 32

        assertThatThrownBy(() -> newService(shortSecret, "schedule", clock))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tối thiểu 32 byte")
                .hasMessageContaining("hiện có 16 byte")
                .message().doesNotContain(shortSecret);
    }

    private void assertErrorCode(String token, ErrorCode expected) {
        assertThatThrownBy(() -> jwtService.parseAccessToken(token))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(expected);
    }

    private static JwtService newService(String secret, String issuer, Clock clock) {
        return new JwtService(new JwtProperties(secret, Duration.ofMinutes(15), Duration.ofDays(7), issuer), clock);
    }

    private static String base64(String raw) {
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** Đồng hồ tua được, để thử token hết hạn mà không phải chờ thật. */
    private static final class MutableClock extends Clock {

        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("Asia/Ho_Chi_Minh");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
