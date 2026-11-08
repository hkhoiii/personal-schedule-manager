package com.dacn.schedule.auth.repository;

import com.dacn.schedule.auth.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.Key> {
}
