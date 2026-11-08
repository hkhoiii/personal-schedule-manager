package com.dacn.schedule.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Người dùng, ánh xạ bảng {@code nguoidung}.
 *
 * <p>Vai trò (bảng {@code vaitronguoidung}) do module auth quản lý, không ánh xạ ở đây để module user
 * không phụ thuộc ngược vào module auth.
 *
 * <p>Thời điểm tạo / cập nhật do service gán bằng {@code Clock}: nếu để {@code null}, Hibernate sẽ ghi
 * NULL đè lên giá trị mặc định {@code CURRENT_TIMESTAMP} của database.
 */
@Entity
@Table(name = "nguoidung")
public class User {

    /** Giá trị cột NguonXacThuc cho tài khoản đăng ký bằng email / mật khẩu. */
    public static final String AUTH_PROVIDER_LOCAL = "LOCAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Luôn lưu dạng chữ thường, đã bỏ khoảng trắng hai đầu. */
    @Column(name = "Email", nullable = false, unique = true, length = 120)
    private String email;

    /** Mã băm BCrypt, không bao giờ lưu mật khẩu gốc. */
    @Column(name = "MatKhau", nullable = false)
    private String passwordHash;

    @Column(name = "HoVaTen", nullable = false, length = 100)
    private String fullName;

    @Column(name = "SoDienThoai", length = 20)
    private String phone;

    @Column(name = "AnhDaiDien", length = 500)
    private String avatarUrl;

    @Column(name = "NguonXacThuc", length = 20)
    private String authProvider = AUTH_PROVIDER_LOCAL;

    @Column(name = "KichHoat")
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "XacThucEmail")
    private Boolean emailVerified = Boolean.FALSE;

    @Column(name = "NgayTao", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "NgayCapNhat")
    private OffsetDateTime updatedAt;

    /** Dành cho JPA. */
    protected User() {
    }

    /** Tạo tài khoản đăng ký bằng email / mật khẩu (NguonXacThuc = LOCAL, đã kích hoạt, chưa xác thực email). */
    public static User newLocalUser(String email, String passwordHash, String fullName, OffsetDateTime now) {
        User user = new User();
        user.email = email;
        user.passwordHash = passwordHash;
        user.fullName = fullName;
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public boolean isEmailVerified() {
        return Boolean.TRUE.equals(emailVerified);
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
