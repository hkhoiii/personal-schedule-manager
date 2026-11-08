package com.dacn.schedule.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Refresh token, ánh xạ bảng {@code tokenlammoi}. Chỉ lưu mã băm SHA-256 của token, không lưu token gốc.
 * Còn hiệu lực khi {@code revokedAt == null} và {@code expiresAt} ở tương lai.
 */
@Entity
@Table(name = "tokenlammoi")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "NguoiDungId", nullable = false)
    private Long userId;

    /** SHA-256 dạng hex (64 ký tự) của token gốc. */
    @Column(name = "MaBamToken", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "HetHanLuc", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "ThuHoiLuc")
    private OffsetDateTime revokedAt;

    @Column(name = "ThietBi")
    private String device;

    @Column(name = "NgayTao", updatable = false)
    private OffsetDateTime createdAt;

    /** Dành cho JPA. */
    protected RefreshToken() {
    }

    public RefreshToken(Long userId, String tokenHash, OffsetDateTime expiresAt, String device, OffsetDateTime now) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.device = device;
        this.createdAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public String getDevice() {
        return device;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
