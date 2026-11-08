package com.dacn.schedule.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dacn.schedule.auth.dto.RegisterRequest;
import com.dacn.schedule.auth.service.AuthService;
import com.dacn.schedule.auth.security.JwtService;
import com.dacn.schedule.common.config.ClockConfig;
import com.dacn.schedule.common.config.SecurityConfig;
import com.dacn.schedule.common.exception.ApiException;
import com.dacn.schedule.common.exception.ErrorCode;
import com.dacn.schedule.common.exception.GlobalExceptionHandler;
import com.dacn.schedule.common.security.RestAccessDeniedHandler;
import com.dacn.schedule.common.security.RestAuthenticationEntryPoint;
import com.dacn.schedule.user.dto.UserResponse;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

/**
 * Kiểm tra tầng web của POST /auth/register: công khai (không cần token), validate dữ liệu đầu vào
 * và trả lỗi đúng định dạng chung. Service được giả lập nên không cần PostgreSQL.
 */
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtService.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class, ClockConfig.class})
class AuthControllerTest {

    private static final String URL = "/api/v1/auth/register";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void validRequest_withoutToken_returns201AndUser() throws Exception {
        given(authService.register(any())).willReturn(new UserResponse(3L, "minhanh@gmail.com", "Nguyễn Minh Anh",
                null, false, List.of("ROLE_USER"), OffsetDateTime.parse("2026-11-03T18:30:00+07:00")));

        mockMvc.perform(register("minhanh@gmail.com", "matkhau123", "Nguyễn Minh Anh"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.email").value("minhanh@gmail.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void invalidEmail_returnsValidationFailed() throws Exception {
        expectFieldError(register("khong-phai-email", "matkhau123", "An"), "email", "Email không đúng định dạng");
    }

    @Test
    void emailWithoutTopLevelDomain_returnsValidationFailed() throws Exception {
        expectFieldError(register("an@localhost", "matkhau123", "An"), "email", "Email không đúng định dạng");
    }

    @Test
    void shortPassword_returnsValidationFailed() throws Exception {
        expectFieldError(register("an@gmail.com", "abc1", "An"), "password", "Mật khẩu phải từ 8 đến 72 ký tự");
    }

    @Test
    void passwordWithoutDigit_returnsValidationFailed() throws Exception {
        expectFieldError(register("an@gmail.com", "chicochucai", "An"), "password",
                "Mật khẩu phải có ít nhất một chữ cái và một chữ số");
    }

    @Test
    void blankFullName_returnsValidationFailed() throws Exception {
        expectFieldError(register("an@gmail.com", "matkhau123", "   "), "fullName", "Họ và tên không được để trống");
    }

    @Test
    void emailAlreadyUsed_returns409() throws Exception {
        given(authService.register(any())).willThrow(new ApiException(ErrorCode.EMAIL_ALREADY_USED));

        mockMvc.perform(register("an@gmail.com", "matkhau123", "An"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"))
                .andExpect(jsonPath("$.message").value("Email này đã được đăng ký."));
    }

    private void expectFieldError(RequestBuilder request, String field, String message) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[?(@.field=='" + field + "')].message").value(message));
        verify(authService, never()).register(any(RegisterRequest.class));
    }

    private static RequestBuilder register(String email, String password, String fullName) {
        String body = """
                {"email":"%s","password":"%s","fullName":"%s"}""".formatted(email, password, fullName);
        return post(URL).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
