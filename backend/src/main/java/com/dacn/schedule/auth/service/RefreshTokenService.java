package com.dacn.schedule.auth.service;

import com.dacn.schedule.auth.entity.RefreshToken;
import com.dacn.schedule.auth.repository.RefreshTokenRepository;
import com.dacn.schedule.auth.security.JwtProperties;
import com.dacn.schedule.common.exception.ApiException;
import com.dacn.schedule.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cấp, xoay vòng và thu hồi refresh token (bảng {@code tokenlammoi}).
 *
 * <p>Refresh token là 32 byte ngẫu nhiên (SecureRandom) mã hóa Base64URL, KHÔNG phải JWT. Database chỉ giữ
 * SHA-256 của nó, nên lộ database cũng không dùng lại được token.
 *
 * <p>Xoay vòng: mỗi token chỉ dùng được một lần. Nếu một token ĐÃ bị thu hồi lại được gửi lên, nhiều khả năng
 * nó đã bị đánh cắp (kẻ gian hoặc người dùng thật đã dùng nó trước), nên thu hồi luôn mọi phiên của người đó.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int TOKEN_BYTES = 32;
    private static final int MAX_DEVICE_LENGTH = 255;

    private final RefreshTokenRepository repository;
    private final Duration ttl;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository, JwtProperties properties, Clock clock) {
        this.repository = repository;
        this.ttl = properties.refreshTokenTtl();
        this.clock = clock;
    }

    /** Token gốc (chỉ trả cho client một lần), thời điểm hết hạn và số giây còn hiệu lực. */
    public record IssuedRefreshToken(String value, OffsetDateTime expiresAt, long expiresInSeconds) {
    }

    /** Cấp refresh token mới cho người dùng. */
    @Transactional
    public IssuedRefreshToken issue(Long userId, String userAgent) {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime expiresAt = now.plus(ttl);
        repository.save(new RefreshToken(userId, sha256Hex(rawToken), expiresAt, truncate(userAgent), now));
        return new IssuedRefreshToken(rawToken, expiresAt, ttl.toSeconds());
    }

    /**
     * Kiểm tra token, thu hồi nó và trả id người dùng để cấp cặp token mới.
     *
     * @throws ApiException TOKEN_EXPIRED nếu token không tồn tại, hết hạn, hoặc đã bị thu hồi
     *                      (trường hợp cuối còn thu hồi mọi phiên của người dùng)
     */
    @Transactional(noRollbackFor = ApiException.class)
    public Long rotate(String rawToken) {
        RefreshToken token = repository.findByTokenHash(sha256Hex(rawToken))
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_EXPIRED));
        OffsetDateTime now = OffsetDateTime.now(clock);

        if (token.getRevokedAt() != null || repository.revokeIfActive(token.getId(), now) == 0) {
            log.warn("Refresh token đã thu hồi bị dùng lại (người dùng {}); thu hồi mọi phiên đăng nhập", token.getUserId());
            repository.revokeAllByUserId(token.getUserId(), now);
            throw new ApiException(ErrorCode.TOKEN_EXPIRED);
        }
        if (!token.getExpiresAt().isAfter(now)) {
            throw new ApiException(ErrorCode.TOKEN_EXPIRED);
        }
        return token.getUserId();
    }

    /** Thu hồi một token (đăng xuất). Token không tồn tại / đã thu hồi thì bỏ qua, không báo lỗi. */
    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(sha256Hex(rawToken))
                .ifPresent(token -> repository.revokeIfActive(token.getId(), OffsetDateTime.now(clock)));
    }

    /** Thu hồi mọi phiên đăng nhập của người dùng (dùng khi đổi mật khẩu, khóa tài khoản). */
    @Transactional
    public void revokeAll(Long userId) {
        repository.revokeAllByUserId(userId, OffsetDateTime.now(clock));
    }

    static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JVM không hỗ trợ SHA-256", ex);
        }
    }

    private static String truncate(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        return userAgent.length() <= MAX_DEVICE_LENGTH ? userAgent : userAgent.substring(0, MAX_DEVICE_LENGTH);
    }
}
