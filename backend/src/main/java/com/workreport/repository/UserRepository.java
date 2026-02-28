package com.workreport.repository;

import com.workreport.entity.User;
import com.workreport.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role AND u.active = true")
    List<User> findByRole(@Param("role") Role role);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role AND u.department.id = :departmentId AND u.active = true")
    List<User> findByRoleAndDepartmentId(@Param("role") Role role, @Param("departmentId") Long departmentId);

    List<User> findByDepartmentId(Long departmentId);
}
