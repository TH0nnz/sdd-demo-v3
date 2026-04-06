package com.workreport.service;

import com.workreport.annotation.PublicApi;
import com.workreport.dto.common.PageResponse;
import com.workreport.dto.notification.NotificationResponse;
import com.workreport.entity.Notification;
import com.workreport.entity.User;
import com.workreport.enums.NotificationType;
import com.workreport.exception.ResourceNotFoundException;
import com.workreport.repository.NotificationRepository;
import com.workreport.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @PublicApi
    @Transactional
    public void notify(Long userId, NotificationType type, String title, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .content(content)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
    }

    @PublicApi
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(Long userId, Boolean unreadOnly, Pageable pageable) {
        Page<Notification> page;
        if (Boolean.TRUE.equals(unreadOnly)) {
            page = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false, pageable);
        } else {
            page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return PageResponse.from(page.map(this::toResponse));
    }

    @PublicApi
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found: " + notificationId);
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @PublicApi
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getContent(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
