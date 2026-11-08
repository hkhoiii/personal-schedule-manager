package com.dacn.schedule.auth.repository;

import com.dacn.schedule.auth.entity.RefreshToken;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Thu hồi một token nếu nó CHƯA bị thu hồi. Trả về số dòng đã cập nhật (0 hoặc 1).
     * Điều kiện {@code revokedAt is null} nằm trong cùng câu UPDATE nên hai yêu cầu làm mới đồng thời bằng
     * cùng một token thì chỉ một yêu cầu thắng.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now where t.id = :id and t.revokedAt is null")
    int revokeIfActive(@Param("id") Long id, @Param("now") OffsetDateTime now);

    /** Thu hồi mọi token còn hiệu lực của một người dùng (đăng xuất mọi nơi / phát hiện token bị đánh cắp). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now where t.userId = :userId and t.revokedAt is null")
    int revokeAllByUserId(@Param("userId") Long userId, @Param("now") OffsetDateTime now);
}
