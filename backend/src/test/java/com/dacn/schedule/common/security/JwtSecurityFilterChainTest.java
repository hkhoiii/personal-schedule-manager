package com.dacn.schedule.common.security;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dacn.schedule.auth.controller.AuthController;
import com.dacn.schedule.auth.dto.TokenResponse;
import com.dacn.schedule.auth.security.JwtProperties;
import com.dacn.schedule.auth.security.JwtService;
import com.dacn.schedule.auth.service.AuthService;
import com.dacn.schedule.common.config.ClockConfig;
import com.dacn.schedule.common.config.SecurityConfig;
import com.dacn.schedule.common.exception.GlobalExceptionHandler;
import com.dacn.schedule.user.controller.UserController;
import com.dacn.schedule.user.entity.User;
import com.dacn.schedule.user.service.UserService;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kiểm tra chuỗi bảo mật thật (SecurityConfig + JwtAuthenticationFilter + JwtService) ở tầng web:
 * endpoint nào mở / bị chặn, lỗi 401 / 403 dạng JSON thống nhất, CORS. Service được giả lập nên không cần PostgreSQL.
 */
@WebMvcTest(controllers = {UserController.class, AuthController.class,
        JwtSecurityFilterChainTest.AdminOnlyController.class})
@Import({SecurityConfig.class, JwtService.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class, ClockConfig.class, JwtSecurityFilterChainTest.AdminOnlyController.class})
class JwtSecurityFilterChainTest {

    private static final String ME = "/api/v1/users/me";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtProperties jwtProperties;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    // ------------------------------------------------------------ GET /users/me: tiêu chí "Xong khi"

    @Test
    void me_withoutToken_returns401Unauthenticated() throws Exception {
        mockMvc.perform(get(ME))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.path").value(ME));
    }

    @Test
    void me_withValidToken_returnsCurrentUser() throws Exception {
        given(userService.findById(7L)).willReturn(Optional.of(user(7L, "an@gmail.com", "Nguyễn Văn An")));
        String token = jwtService.generateAccessToken(7L, "an@gmail.com", List.of("ROLE_USER"));

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("an@gmail.com"))
                .andExpect(jsonPath("$.fullName").value("Nguyễn Văn An"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void bearerPrefixIsCaseInsensitive() throws Exception {
        given(userService.findById(7L)).willReturn(Optional.of(user(7L, "an@gmail.com", "An")));
        String token = jwtService.generateAccessToken(7L, "an@gmail.com", List.of("ROLE_USER"));

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void me_withExpiredToken_returns401TokenExpired() throws Exception {
        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }

    @Test
    void me_withTamperedToken_returns401InvalidToken() throws Exception {
        String token = jwtService.generateAccessToken(7L, "an@gmail.com", List.of("ROLE_USER"));
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value(containsString("Token không hợp lệ")));
    }

    @Test
    void me_withNonBearerAuthorization_returns401Unauthenticated() throws Exception {
        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Basic YWRtaW46MTIz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void me_tokenStillValidButAccountDeleted_returns401() throws Exception {
        given(userService.findById(99L)).willReturn(Optional.empty());
        String token = jwtService.generateAccessToken(99L, "da.xoa@gmail.com", List.of("ROLE_USER"));

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    // ------------------------------------------------------------ 403

    @Test
    void roleProtectedEndpoint_withUserRole_returns403Json() throws Exception {
        String token = jwtService.generateAccessToken(7L, "an@gmail.com", List.of("ROLE_USER"));

        mockMvc.perform(get("/api/v1/test-admin").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/api/v1/test-admin"));
    }

    @Test
    void roleProtectedEndpoint_withAdminRole_returns200() throws Exception {
        String token = jwtService.generateAccessToken(1L, "admin@gmail.com", List.of("ROLE_ADMIN"));

        mockMvc.perform(get("/api/v1/test-admin").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------ /auth/** công khai

    @Test
    void login_isPublic_andBadCredentialsReturns401Json() throws Exception {
        given(authService.login(any(), any())).willThrow(new BadCredentialsException("sai"));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"an@gmail.com\",\"password\":\"saimatkhau1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không đúng."));
    }

    @Test
    void login_blankFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.length()").value(2));
    }

    @Test
    void refresh_withExpiredAccessTokenInHeader_stillReachesPublicEndpoint() throws Exception {
        // Tình huống thật: access token vừa hết hạn, frontend vẫn gửi kèm header khi gọi /auth/refresh
        given(authService.refresh(anyString(), any()))
                .willReturn(new TokenResponse("access-moi", "Bearer", 900, "refresh-moi", 604800));

        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken())
                        .content("{\"refreshToken\":\"refresh-cu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-moi"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-moi"));
    }

    @Test
    void logout_returns204() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-cu\"}"))
                .andExpect(status().isNoContent());
        verify(authService).logout("refresh-cu");
    }

    // ------------------------------------------------------------ CORS

    @Test
    void cors_requestFromFrontendOrigin_getsAllowOriginHeader() throws Exception {
        given(userService.findById(7L)).willReturn(Optional.of(user(7L, "an@gmail.com", "An")));
        String token = jwtService.generateAccessToken(7L, "an@gmail.com", List.of("ROLE_USER"));

        mockMvc.perform(get(ME).header("Origin", "http://localhost:5173")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void cors_preflightFromUnknownOrigin_isRejected() throws Exception {
        mockMvc.perform(options(ME)
                        .header("Origin", "http://trang-la.com")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    // ------------------------------------------------------------ tiện ích

    /** Token ký đúng khóa nhưng cấp từ 1 giờ trước (sống 15 phút) nên đã hết hạn. */
    private String expiredToken() {
        Clock anHourAgo = Clock.fixed(Instant.now().minusSeconds(3600), ZoneOffset.UTC);
        return new JwtService(jwtProperties, anHourAgo).generateAccessToken(7L, "an@gmail.com", List.of("ROLE_USER"));
    }

    private static User user(Long id, String email, String fullName) {
        User user = User.newLocalUser(email, "$2a$10$hash", fullName, OffsetDateTime.parse("2026-11-04T08:00:00+07:00"));
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    /** Endpoint giả chỉ dành cho ROLE_ADMIN, để kiểm tra lỗi 403. */
    @RestController
    static class AdminOnlyController {

        @GetMapping("/api/v1/test-admin")
        @PreAuthorize("hasRole('ADMIN')")
        public String adminOnly() {
            return "ok";
        }
    }
}
