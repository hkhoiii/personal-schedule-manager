package com.dacn.schedule.auth.repository;

import com.dacn.schedule.auth.entity.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByName(String name);

    /** Tên các vai trò của một người dùng (qua bảng vaitronguoidung), ví dụ ["ROLE_USER"]. */
    @Query("select r.name from Role r, UserRole ur where ur.roleId = r.id and ur.userId = :userId order by r.name")
    List<String> findNamesByUserId(@Param("userId") Long userId);
}
