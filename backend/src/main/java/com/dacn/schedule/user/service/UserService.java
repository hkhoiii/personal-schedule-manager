package com.dacn.schedule.user.service;

import com.dacn.schedule.common.exception.ApiException;
import com.dacn.schedule.common.exception.ErrorCode;
import com.dacn.schedule.user.entity.User;
import com.dacn.schedule.user.repository.UserRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ tài khoản người dùng. Là cửa duy nhất để module khác (auth...) đọc / tạo người dùng.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final Clock clock;

    public UserService(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /** Chuẩn hóa email trước khi lưu hoặc tìm: bỏ khoảng trắng hai đầu, đổi sang chữ thường. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(normalizeEmail(email));
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email));
    }

    /**
     * Tạo tài khoản đăng ký bằng email / mật khẩu.
     *
     * @param passwordHash mật khẩu ĐÃ băm BCrypt (service này không nhận mật khẩu gốc)
     * @throws ApiException EMAIL_ALREADY_USED nếu email đã tồn tại
     */
    @Transactional
    public User createLocalUser(String email, String passwordHash, String fullName) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_USED);
        }
        User user = User.newLocalUser(normalizedEmail, passwordHash, fullName.trim(), OffsetDateTime.now(clock));
        try {
            // flush ngay để lỗi trùng email (hai người đăng ký cùng lúc) lộ ra tại đây, không phải lúc commit
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_USED, null, ex);
        }
    }
}
