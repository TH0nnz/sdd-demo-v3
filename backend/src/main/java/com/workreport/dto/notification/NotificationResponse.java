package com.workreport.dto.notification;

import com.workreport.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String content,
        boolean isRead,
        LocalDateTime createdAt
) {
}
