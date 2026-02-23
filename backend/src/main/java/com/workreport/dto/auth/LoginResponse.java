package com.workreport.dto.auth;

import com.workreport.enums.Role;

import java.util.Set;

public record LoginResponse(
        String token,
        UserInfo user,
        boolean forcePasswordChange
) {
    public record UserInfo(
            Long id,
            String name,
            String email,
            Set<Role> roles,
            Long departmentId,
            String departmentName
    ) {
    }
}
