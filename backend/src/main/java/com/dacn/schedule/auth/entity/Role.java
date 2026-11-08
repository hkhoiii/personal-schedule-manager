package com.dacn.schedule.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Vai trò phân quyền, ánh xạ bảng {@code vaitro}. Dữ liệu cố định, nạp sẵn từ script khởi tạo. */
@Entity
@Table(name = "vaitro")
public class Role {

    /** Vai trò mặc định gán cho mọi tài khoản tự đăng ký. */
    public static final String USER = "ROLE_USER";

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "TenVaiTro", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "MoTa")
    private String description;

    /** Dành cho JPA. */
    protected Role() {
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
