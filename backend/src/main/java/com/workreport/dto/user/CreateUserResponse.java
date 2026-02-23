package com.workreport.dto.user;

public record CreateUserResponse(UserResponse user, String temporaryPassword) {
}
