package com.dacn.schedule.user.repository;

import com.dacn.schedule.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Truy cập bảng {@code nguoidung}. Chỉ dùng trong module user; module khác gọi qua {@code UserService}.
 *
 * <p>Email luôn được lưu dạng chữ thường, nên khi tìm phải chuẩn hóa email trước
 * (xem {@code UserService#normalizeEmail}).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}
