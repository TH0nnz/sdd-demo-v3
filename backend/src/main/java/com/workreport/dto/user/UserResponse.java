package com.workreport.dto.user;

import com.workreport.entity.User;
import com.workreport.enums.Role;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
        Long id,
        String name,
        String email,
        String departmentName,
        Long departmentId,
        Set<Role> roles,
        boolean active,
        boolean locked,
        LocalDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getDepartment().getName(),
                user.getDepartment().getId(),
                user.getRoles(),
                user.isActive(),
                user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now()),
                user.getCreatedAt()
        );
    }
}
