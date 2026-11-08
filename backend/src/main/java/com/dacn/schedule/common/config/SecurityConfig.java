package com.dacn.schedule.common.config;

import com.dacn.schedule.auth.security.JwtAuthenticationFilter;
import com.dacn.schedule.auth.security.JwtProperties;
import com.dacn.schedule.auth.security.JwtService;
import com.dacn.schedule.common.api.ApiPaths;
import com.dacn.schedule.common.security.RestAccessDeniedHandler;
import com.dacn.schedule.common.security.RestAuthenticationEntryPoint;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Cấu hình bảo mật. API stateless (không session, không cookie), xác thực bằng access token JWT trong header
 * {@code Authorization: Bearer ...}.
 *
 * <ul>
 *   <li>Mở (không cần token): {@code /auth/**} (đăng ký, đăng nhập, làm mới, đăng xuất), Swagger,
 *       {@code /system/**} (ping). Mọi đường dẫn khác cần access token hợp lệ.</li>
 *   <li>Chưa đăng nhập / token lỗi: 401 JSON ({@code UNAUTHENTICATED} hoặc {@code TOKEN_EXPIRED}) qua
 *       {@link RestAuthenticationEntryPoint}. Đủ đăng nhập nhưng thiếu quyền: 403 JSON {@code FORBIDDEN} qua
 *       {@link RestAccessDeniedHandler}.</li>
 *   <li>{@code @EnableMethodSecurity}: cho phép giới hạn theo vai trò ngay trên phương thức controller,
 *       ví dụ {@code @PreAuthorize("hasRole('ADMIN')")}.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /** Các đường dẫn không cần đăng nhập. Thêm vào đây khi có endpoint công khai mới. */
    private static final String[] PUBLIC_PATHS = {
            ApiPaths.V1 + "/system/**",
            ApiPaths.V1 + "/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Đăng xuất là POST /api/v1/auth/logout (thu hồi refresh token); tắt /logout mặc định của Spring
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                // Tạo trực tiếp (không phải bean) để Spring Boot không đăng ký bộ lọc này thêm lần nữa vào servlet
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Mã hóa mật khẩu bằng BCrypt (cost 10, chuỗi băm dạng {@code $2a$10$...}). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager dùng cho đăng nhập. Spring Security tự dựng nó từ bean {@code UserDetailsService}
     * duy nhất ({@code AccountUserDetailsService}, đọc bảng nguoidung) và {@link #passwordEncoder()}.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Cho phép frontend (Vite, mặc định http://localhost:5173) gọi API từ trình duyệt.
     * Cấu hình bằng {@code app.cors.allowed-origins}, ngăn cách bằng dấu phẩy. Không cần cookie nên không bật
     * allowCredentials: token đi trong header Authorization.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String[] allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
