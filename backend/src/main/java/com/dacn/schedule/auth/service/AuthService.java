package com.dacn.schedule.auth.service;

import com.dacn.schedule.auth.dto.LoginRequest;
import com.dacn.schedule.auth.dto.RegisterRequest;
import com.dacn.schedule.auth.dto.TokenResponse;
import com.dacn.schedule.auth.entity.Role;
import com.dacn.schedule.auth.entity.UserRole;
import com.dacn.schedule.auth.repository.RoleRepository;
import com.dacn.schedule.auth.repository.UserRoleRepository;
import com.dacn.schedule.auth.security.AccountUserDetails;
import com.dacn.schedule.auth.security.JwtService;
import com.dacn.schedule.common.api.ApiFieldError;
import com.dacn.schedule.common.exception.ApiException;
import com.dacn.schedule.common.exception.ErrorCode;
import com.dacn.schedule.user.dto.UserResponse;
import com.dacn.schedule.user.entity.User;
import com.dacn.schedule.user.service.UserService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nghiệp vụ xác thực: đăng ký, đăng nhập, làm mới token, đăng xuất. */
@Service
public class AuthService {

    /** BCrypt chỉ dùng 72 byte đầu của mật khẩu; Spring Security từ chối mật khẩu dài hơn. */
    static final int BCRYPT_MAX_BYTES = 72;
    private static final String TOKEN_TYPE = "Bearer";

    private final UserService userService;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserService userService, RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Đăng ký tài khoản mới với vai trò ROLE_USER. Mật khẩu được băm BCrypt trước khi lưu.
     *
     * @throws ApiException EMAIL_ALREADY_USED nếu email đã được đăng ký;
     *                      VALIDATION_FAILED nếu mật khẩu vượt 72 byte
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (exceedsBcryptLimit(request.password())) {
            // @Size đếm ký tự; chữ có dấu tiếng Việt chiếm 2-3 byte nên vẫn có thể vượt giới hạn của BCrypt
            throw new ApiException(ErrorCode.VALIDATION_FAILED, null,
                    List.of(new ApiFieldError("password", "Mật khẩu quá dài, vui lòng dùng mật khẩu ngắn hơn")));
        }

        Role userRole = roleRepository.findByName(Role.USER)
                .orElseThrow(() -> new IllegalStateException(
                        "Bảng vaitro thiếu " + Role.USER + ", hãy chạy lại database/init_db_quanlycongviec.sql"));

        User user = userService.createLocalUser(
                request.email(), passwordEncoder.encode(request.password()), request.fullName());
        userRoleRepository.save(new UserRole(user.getId(), userRole.getId()));

        return UserResponse.from(user, List.of(userRole.getName()));
    }

    /**
     * Đăng nhập bằng email / mật khẩu, trả access token + refresh token.
     *
     * @throws BadCredentialsException (401 BAD_CREDENTIALS) nếu email không tồn tại hoặc sai mật khẩu
     */
    @Transactional
    public TokenResponse login(LoginRequest request, String userAgent) {
        if (exceedsBcryptLimit(request.password())) {
            // Không thể là mật khẩu đúng (đăng ký đã chặn); trả lỗi như sai mật khẩu thay vì để BCrypt ném lỗi 500
            throw new BadCredentialsException("Mật khẩu vượt giới hạn BCrypt");
        }
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        AccountUserDetails account = (AccountUserDetails) authentication.getPrincipal();

        return issueTokens(account.getUserId(), account.getUsername(), account.getRoles(), userAgent);
    }

    /**
     * Đổi refresh token lấy cặp token mới; refresh token cũ bị thu hồi (xoay vòng).
     *
     * @throws ApiException TOKEN_EXPIRED nếu refresh token sai / hết hạn / đã dùng, hoặc tài khoản đã bị khóa
     */
    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse refresh(String refreshToken, String userAgent) {
        Long userId = refreshTokenService.rotate(refreshToken);
        User user = userService.findById(userId)
                .filter(User::isEnabled)
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_EXPIRED));

        return issueTokens(user.getId(), user.getEmail(), roleRepository.findNamesByUserId(user.getId()), userAgent);
    }

    /** Đăng xuất: thu hồi refresh token. Gọi lại nhiều lần hoặc gửi token đã hết hạn vẫn thành công. */
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private TokenResponse issueTokens(Long userId, String email, List<String> roles, String userAgent) {
        String accessToken = jwtService.generateAccessToken(userId, email, roles);
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(userId, userAgent);

        return new TokenResponse(accessToken, TOKEN_TYPE, jwtService.getAccessTokenTtl().toSeconds(),
                refreshToken.value(), refreshToken.expiresInSeconds());
    }

    private static boolean exceedsBcryptLimit(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
    }
}
