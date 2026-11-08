package com.dacn.schedule.auth.security;

import com.dacn.schedule.auth.repository.RoleRepository;
import com.dacn.schedule.user.entity.User;
import com.dacn.schedule.user.service.UserService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nguồn tài khoản cho Spring Security: đọc người dùng (qua {@link UserService}) và vai trò (bảng vaitronguoidung).
 *
 * <p>Spring Security tự dùng bean này cùng {@code PasswordEncoder} (BCrypt) để dựng AuthenticationManager.
 * Email không tồn tại hay mật khẩu sai đều trả về cùng một lỗi BAD_CREDENTIALS, và Spring vẫn chạy BCrypt
 * giả khi không thấy email, nên không thể dò email nào đã đăng ký qua nội dung lỗi hay thời gian phản hồi.
 */
@Service
public class AccountUserDetailsService implements UserDetailsService {

    private final UserService userService;
    private final RoleRepository roleRepository;

    public AccountUserDetailsService(UserService userService, RoleRepository roleRepository) {
        this.userService = userService;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountUserDetails loadUserByUsername(String email) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
        return new AccountUserDetails(user.getId(), user.getEmail(), user.getPasswordHash(), user.isEnabled(),
                roleRepository.findNamesByUserId(user.getId()));
    }
}
