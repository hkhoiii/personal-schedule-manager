package com.dacn.schedule.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dacn.schedule.auth.dto.RegisterRequest;
import com.dacn.schedule.auth.entity.UserRole;
import com.dacn.schedule.auth.repository.RoleRepository;
import com.dacn.schedule.auth.repository.UserRoleRepository;
import com.dacn.schedule.common.exception.ApiException;
import com.dacn.schedule.common.exception.ErrorCode;
import com.dacn.schedule.user.dto.UserResponse;
import com.dacn.schedule.user.entity.User;
import com.dacn.schedule.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đăng ký với PostgreSQL thật: kiểm tra ánh xạ entity với bảng nguoidung / vaitronguoidung,
 * băm BCrypt và gán vai trò. Cần PostgreSQL đang chạy (giống ScheduleApplicationTests).
 * Mỗi test chạy trong một transaction và được rollback, không để lại dữ liệu trong database.
 */
@SpringBootTest
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void register_savesBcryptHashNormalizedEmailAndRoleUser() {
        UserResponse response = authService.register(
                new RegisterRequest("  Test.DangKy@Gmail.COM ", "matkhau123", "  Người Dùng Test  "));

        assertThat(response.id()).isNotNull();
        assertThat(response.email()).isEqualTo("test.dangky@gmail.com");
        assertThat(response.fullName()).isEqualTo("Người Dùng Test");
        assertThat(response.roles()).containsExactly("ROLE_USER");
        assertThat(response.createdAt()).isNotNull();

        User saved = userService.findByEmail("test.dangky@gmail.com").orElseThrow();
        assertThat(saved.getPasswordHash()).startsWith("$2").isNotEqualTo("matkhau123");
        assertThat(passwordEncoder.matches("matkhau123", saved.getPasswordHash())).isTrue();
        assertThat(saved.getAuthProvider()).isEqualTo(User.AUTH_PROVIDER_LOCAL);
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.isEmailVerified()).isFalse();

        Integer roleUserId = roleRepository.findByName("ROLE_USER").orElseThrow().getId();
        assertThat(userRoleRepository.existsById(new UserRole.Key(saved.getId(), roleUserId))).isTrue();
    }

    @Test
    void register_sameEmailDifferentCase_throwsEmailAlreadyUsed() {
        authService.register(new RegisterRequest("trung.email@gmail.com", "matkhau123", "Lần Một"));

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("TRUNG.EMAIL@gmail.com", "matkhau456", "Lần Hai")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_USED);
    }

    @Test
    void register_passwordOver72Bytes_throwsValidationFailed() {
        // 30 chữ "ữ" (3 byte mỗi chữ) + "1" = 31 ký tự (qua được @Size) nhưng 91 byte
        String longVietnamesePassword = "ữ".repeat(30) + "1";

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("dai.qua@gmail.com", longVietnamesePassword, "Mật Khẩu Dài")))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiEx = (ApiException) ex;
                    assertThat(apiEx.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                    assertThat(apiEx.getDetails()).extracting("field").containsExactly("password");
                });
        assertThat(userService.existsByEmail("dai.qua@gmail.com")).isFalse();
    }
}
