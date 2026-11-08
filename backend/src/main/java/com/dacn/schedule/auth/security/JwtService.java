package com.dacn.schedule.auth.security;

import com.dacn.schedule.common.exception.ApiException;
import com.dacn.schedule.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Tạo và kiểm tra access token (JWT, ký HS256) bằng thư viện jjwt.
 *
 * <p>Nội dung token: {@code sub} = id người dùng, {@code email}, {@code roles}, {@code iss}, {@code iat},
 * {@code exp}, {@code jti}. Access token KHÔNG lưu database; refresh token là chuỗi ngẫu nhiên lưu ở bảng
 * {@code tokenlammoi}, không phải JWT.
 *
 * <p>Khóa ký lấy từ biến môi trường {@code JWT_SECRET} (qua {@code app.jwt.secret}). Thiếu hoặc yếu thì
 * ứng dụng dừng ngay lúc khởi động, thay vì chạy với khóa không an toàn.
 *
 * <p>Thời gian lấy từ {@link Clock} dùng chung, nên test có thể cố định / tua thời gian.
 */
@Service
public class JwtService {

    /** HS256 yêu cầu khóa tối thiểu 256 bit. */
    static final int MIN_SECRET_BYTES = 32;

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLES = "roles";

    private final SecretKey signingKey;
    private final Duration accessTokenTtl;
    private final String issuer;
    private final Clock clock;
    private final JwtParser parser;

    public JwtService(JwtProperties properties, Clock clock) {
        this.signingKey = buildSigningKey(properties.secret());
        this.accessTokenTtl = properties.accessTokenTtl();
        this.issuer = properties.issuer();
        this.clock = clock;
        this.parser = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    /** Tạo access token cho người dùng; hết hạn sau {@link #getAccessTokenTtl()}. */
    public String generateAccessToken(Long userId, String email, Collection<String> roles) {
        Instant now = clock.instant();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLES, List.copyOf(roles))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Kiểm tra chữ ký, hạn dùng, issuer rồi trả thông tin trong token.
     *
     * @throws ApiException TOKEN_EXPIRED nếu token đã hết hạn;
     *                      UNAUTHENTICATED nếu token sai chữ ký, sai định dạng, không ký, khác issuer...
     */
    public AccessTokenClaims parseAccessToken(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            return new AccessTokenClaims(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_EMAIL, String.class),
                    readRoles(claims),
                    claims.getExpiration().toInstant());
        } catch (ExpiredJwtException ex) {
            throw new ApiException(ErrorCode.TOKEN_EXPIRED, null, ex);
        } catch (JwtException | IllegalArgumentException ex) {
            // IllegalArgumentException: token rỗng, hoặc sub không phải số
            throw new ApiException(ErrorCode.UNAUTHENTICATED, "Token không hợp lệ, vui lòng đăng nhập lại.", ex);
        }
    }

    /** Thời gian sống của access token (để trả {@code expiresIn} cho client khi đăng nhập). */
    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    private static List<String> readRoles(Claims claims) {
        Object value = claims.get(CLAIM_ROLES);
        if (!(value instanceof Collection<?> collection)) {
            return List.of();
        }
        return collection.stream().map(String::valueOf).toList();
    }

    /** Thông báo lỗi không bao giờ chứa giá trị khóa. */
    static SecretKey buildSigningKey(String base64Secret) {
        String howToCreate = " Tạo khóa bằng lệnh \"openssl rand -base64 48\" (Git Bash) rồi đặt vào JWT_SECRET trong file .env.";
        if (base64Secret == null || base64Secret.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình JWT_SECRET (app.jwt.secret)." + howToCreate);
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(base64Secret.trim());
        } catch (DecodingException ex) {
            throw new IllegalStateException("JWT_SECRET phải là chuỗi Base64." + howToCreate, ex);
        }
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET quá ngắn: cần tối thiểu " + MIN_SECRET_BYTES
                    + " byte sau khi giải mã Base64, hiện có " + keyBytes.length + " byte." + howToCreate);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
