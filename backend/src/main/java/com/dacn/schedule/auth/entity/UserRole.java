package com.dacn.schedule.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * Gán vai trò cho người dùng, ánh xạ bảng {@code vaitronguoidung} (khóa chính gồm hai cột).
 *
 * <p>Chỉ lưu id, không ánh xạ quan hệ tới {@code User} để module auth không phải đọc thẳng
 * bảng của module user.
 */
@Entity
@Table(name = "vaitronguoidung")
@IdClass(UserRole.Key.class)
public class UserRole {

    @Id
    @Column(name = "NguoiDungId")
    private Long userId;

    @Id
    @Column(name = "VaiTroId")
    private Integer roleId;

    /** Dành cho JPA. */
    protected UserRole() {
    }

    public UserRole(Long userId, Integer roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getRoleId() {
        return roleId;
    }

    /** Khóa chính kết hợp (NguoiDungId, VaiTroId). */
    public static class Key implements Serializable {

        private Long userId;
        private Integer roleId;

        public Key() {
        }

        public Key(Long userId, Integer roleId) {
            this.userId = userId;
            this.roleId = roleId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key other
                    && Objects.equals(userId, other.userId)
                    && Objects.equals(roleId, other.roleId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, roleId);
        }
    }
}
