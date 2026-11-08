package com.dacn.schedule.auth.security;

import com.dacn.schedule.common.exception.ApiException;
import com.dacn.schedule.common.security.AuthenticatedUser;
import com.dacn.schedule.common.security.RestAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Đọc header {@code Authorization: Bearer <access token>}, kiểm tra bằng {@link JwtService} và đặt
 * {@link AuthenticatedUser} vào SecurityContext.
 *
 * <ul>
 *   <li>Không có header Bearer: cho đi tiếp, chưa đăng nhập. Endpoint công khai vẫn chạy; endpoint cần đăng nhập
 *       bị {@code RestAuthenticationEntryPoint} trả 401 UNAUTHENTICATED.</li>
 *   <li>Token lỗi (hết hạn, sai chữ ký...): KHÔNG chặn ngay mà ghi lỗi vào request rồi cho đi tiếp, chưa đăng nhập.
 *       Nhờ vậy endpoint công khai (ví dụ /auth/refresh khi access token vừa hết hạn) vẫn gọi được; endpoint cần
 *       đăng nhập trả đúng lỗi TOKEN_EXPIRED / UNAUTHENTICATED.</li>
 * </ul>
 *
 * <p>Không truy vấn database: mọi thông tin lấy từ token đã ký. Vì thế thay đổi vai trò / khóa tài khoản chỉ có
 * hiệu lực khi access token cũ hết hạn (tối đa {@code app.jwt.access-token-ttl}).
 *
 * <p>KHÔNG khai báo lớp này là {@code @Component}: Spring Boot sẽ tự đăng ký mọi bean Filter vào servlet, khiến bộ
 * lọc chạy hai lần. {@code SecurityConfig} tự tạo và gắn nó vào chuỗi bảo mật.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();
    private final WebAuthenticationDetailsSource detailsSource = new WebAuthenticationDetailsSource();

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            chain.doFilter(request, response);
            return;
        }

        try {
            AccessTokenClaims claims = jwtService.parseAccessToken(header.substring(BEARER_PREFIX.length()).trim());
            AuthenticatedUser principal = new AuthenticatedUser(claims.userId(), claims.email(), claims.roles());
            List<SimpleGrantedAuthority> authorities = claims.roles().stream().map(SimpleGrantedAuthority::new).toList();

            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
            authentication.setDetails(detailsSource.buildDetails(request));

            SecurityContext context = contextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            contextHolder.setContext(context);
        } catch (ApiException ex) {
            contextHolder.clearContext();
            request.setAttribute(RestAuthenticationEntryPoint.TOKEN_ERROR_ATTRIBUTE, ex);
        }

        chain.doFilter(request, response);
    }
}
