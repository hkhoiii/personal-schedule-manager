package com.dacn.schedule.common.config;

import com.dacn.schedule.common.api.ApiPaths;
import com.dacn.schedule.common.security.RestAccessDeniedHandler;
import com.dacn.schedule.common.security.RestAuthenticationEntryPoint;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Cấu hình bảo mật khung (Ngày 6). API stateless, dùng Bearer token; bộ lọc JWT sẽ được thêm ở phần
 * Xác thực (module {@code auth}).
 *
 * <p>Hiện tại: Swagger, {@code /system/**} và {@code /auth/**} được mở; mọi endpoint khác yêu cầu
 * đăng nhập và trả lỗi JSON {@code UNAUTHENTICATED} (401) nếu chưa có token hợp lệ.
 */
@Configuration
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
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    /** Mã hóa mật khẩu bằng BCrypt (khớp với mật khẩu mẫu {@code $2a$10$...} trong database). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Tạm thời từ chối mọi tài khoản, chỉ để Spring Boot không tự tạo người dùng "user" với mật khẩu
     * ngẫu nhiên in ra log. XÓA bean này khi viết {@code CustomUserDetailsService} đọc từ bảng nguoidung.
     */
    @Bean
    public UserDetailsService placeholderUserDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Chưa cấu hình nguồn người dùng");
        };
    }

    /** Cho phép frontend (Vite) gọi API. Cấu hình bằng {@code app.cors.allowed-origins}, ngăn cách bằng dấu phẩy. */
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
