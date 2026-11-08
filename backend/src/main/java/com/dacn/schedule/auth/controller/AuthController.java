package com.dacn.schedule.auth.controller;

import com.dacn.schedule.auth.dto.LoginRequest;
import com.dacn.schedule.auth.dto.RefreshTokenRequest;
import com.dacn.schedule.auth.dto.RegisterRequest;
import com.dacn.schedule.auth.dto.TokenResponse;
import com.dacn.schedule.auth.service.AuthService;
import com.dacn.schedule.common.api.ApiPaths;
import com.dacn.schedule.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Mọi endpoint ở đây đều công khai (mở trong SecurityConfig qua /auth/**). */
@RestController
@RequestMapping(ApiPaths.V1 + "/auth")
@Tag(name = "Xác thực", description = "Đăng ký, đăng nhập, làm mới và thu hồi token")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Đăng ký tài khoản",
            description = "Không cần đăng nhập. Tạo tài khoản với vai trò ROLE_USER, mật khẩu được băm BCrypt.")
    @ApiResponse(responseCode = "201", description = "Đăng ký thành công")
    @ApiResponse(responseCode = "409", description = "Email đã được đăng ký (EMAIL_ALREADY_USED)")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Đăng nhập",
            description = "Trả access token (JWT, dùng trong header Authorization: Bearer ...) và refresh token.")
    @ApiResponse(responseCode = "200", description = "Đăng nhập thành công")
    @ApiResponse(responseCode = "401", description = "Sai email hoặc mật khẩu (BAD_CREDENTIALS)")
    public TokenResponse login(@Valid @RequestBody LoginRequest request,
                               @Parameter(hidden = true) @RequestHeader(value = HttpHeaders.USER_AGENT, required = false)
                               String userAgent) {
        return authService.login(request, userAgent);
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Làm mới token",
            description = "Đổi refresh token lấy cặp token mới. Refresh token cũ bị thu hồi, hãy lưu token mới.")
    @ApiResponse(responseCode = "200", description = "Cấp cặp token mới")
    @ApiResponse(responseCode = "401", description = "Refresh token sai, hết hạn hoặc đã dùng (TOKEN_EXPIRED)")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request,
                                 @Parameter(hidden = true) @RequestHeader(value = HttpHeaders.USER_AGENT, required = false)
                                 String userAgent) {
        return authService.refresh(request.refreshToken(), userAgent);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirements
    @Operation(summary = "Đăng xuất",
            description = "Thu hồi refresh token. Access token cũ tự hết hạn sau tối đa 15 phút; client nên xóa nó ngay.")
    @ApiResponse(responseCode = "204", description = "Đã đăng xuất")
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
    }
}
