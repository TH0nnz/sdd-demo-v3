package com.workreport.unit.service;

import com.workreport.dto.common.PageResponse;
import com.workreport.dto.notification.NotificationResponse;
import com.workreport.entity.Notification;
import com.workreport.entity.User;
import com.workreport.enums.NotificationType;
import com.workreport.exception.ResourceNotFoundException;
import com.workreport.repository.NotificationRepository;
import com.workreport.repository.UserRepository;
import com.workreport.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService")
class NotificationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long NOTIFICATION_ID = 10L;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    private NotificationService notificationService;

    private User user;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, userRepository);
        user = new User();
        user.setId(USER_ID);
        user.setName("Test User");
    }

    @Nested
    @DisplayName("notify")
    class Notify {

        @Test
        @DisplayName("user 不存在時拋出 ResourceNotFoundException")
        void whenUserNotFound_throwsResourceNotFoundException() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.notify(
                    USER_ID, NotificationType.HOURS_REQUEST_APPROVED, "Title", "Content"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found: " + USER_ID);

            verify(userRepository).findById(USER_ID);
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("user 存在時建立 Notification 並 save")
        void whenUserExists_createsAndSavesNotification() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            notificationService.notify(
                    USER_ID,
                    NotificationType.HOURS_REQUEST_SUBMITTED,
                    "Hours Request",
                    "Your request was submitted");

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());

            Notification saved = captor.getValue();
            assertThat(saved.getUser()).isSameAs(user);
            assertThat(saved.getType()).isEqualTo(NotificationType.HOURS_REQUEST_SUBMITTED);
            assertThat(saved.getTitle()).isEqualTo("Hours Request");
            assertThat(saved.getContent()).isEqualTo("Your request was submitted");
            assertThat(saved.isRead()).isFalse();
            assertThat(saved.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("getNotifications")
    class GetNotifications {

        @Test
        @DisplayName("unreadOnly=true 時使用 findByUserIdAndIsReadOrderByCreatedAtDesc 並回傳 PageResponse")
        void whenUnreadOnlyTrue_usesFindByUserIdAndIsReadAndReturnsPageResponse() {
            Pageable pageable = PageRequest.of(0, 10);
            Notification n = notification(USER_ID, false);
            when(notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(eq(USER_ID), eq(false), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(n), pageable, 1));

            PageResponse<NotificationResponse> result =
                    notificationService.getNotifications(USER_ID, true, pageable);

            verify(notificationRepository).findByUserIdAndIsReadOrderByCreatedAtDesc(USER_ID, false, pageable);
            verify(notificationRepository, never()).findByUserIdOrderByCreatedAtDesc(any(), any());
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).type()).isEqualTo(n.getType());
            assertThat(result.content().get(0).title()).isEqualTo(n.getTitle());
            assertThat(result.content().get(0).isRead()).isFalse();
            assertThat(result.totalElements()).isOne();
            assertThat(result.page()).isZero();
            assertThat(result.size()).isEqualTo(10);
        }

        @Test
        @DisplayName("unreadOnly=false 時使用 findByUserIdOrderByCreatedAtDesc 並回傳 PageResponse")
        void whenUnreadOnlyFalse_usesFindByUserIdAndReturnsPageResponse() {
            Pageable pageable = PageRequest.of(0, 20);
            Notification n = notification(USER_ID, true);
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(USER_ID), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(n), pageable, 1));

            PageResponse<NotificationResponse> result =
                    notificationService.getNotifications(USER_ID, false, pageable);

            verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(USER_ID, pageable);
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).isRead()).isTrue();
            assertThat(result.totalElements()).isOne();
            assertThat(result.size()).isEqualTo(20);
        }

        @Test
        @DisplayName("unreadOnly=null 時使用 findByUserIdOrderByCreatedAtDesc")
        void whenUnreadOnlyNull_usesFindByUserIdOrderByCreatedAtDesc() {
            Pageable pageable = PageRequest.of(0, 10);
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(USER_ID), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            notificationService.getNotifications(USER_ID, null, pageable);

            verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(USER_ID, pageable);
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("notification 不存在時拋出 ResourceNotFoundException")
        void whenNotificationNotFound_throwsResourceNotFoundException() {
            when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markAsRead(NOTIFICATION_ID, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Notification not found: " + NOTIFICATION_ID);

            verify(notificationRepository).findById(NOTIFICATION_ID);
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("notification 不屬於該 user 時拋出 ResourceNotFoundException")
        void whenNotificationBelongsToOtherUser_throwsResourceNotFoundException() {
            Notification notification = notification(OTHER_USER_ID, false);
            notification.setId(NOTIFICATION_ID);
            when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

            assertThatThrownBy(() -> notificationService.markAsRead(NOTIFICATION_ID, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Notification not found: " + NOTIFICATION_ID);

            verify(notificationRepository).findById(NOTIFICATION_ID);
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("notification 存在且屬於該 user 時 setRead(true) 並 save")
        void whenNotificationBelongsToUser_setsReadAndSaves() {
            Notification notification = notification(USER_ID, false);
            notification.setId(NOTIFICATION_ID);
            when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

            notificationService.markAsRead(NOTIFICATION_ID, USER_ID);

            assertThat(notification.isRead()).isTrue();
            verify(notificationRepository).save(notification);
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCount {

        @Test
        @DisplayName("回傳 countByUserIdAndIsRead(userId, false)")
        void returnsCountByUserIdAndIsReadFalse() {
            when(notificationRepository.countByUserIdAndIsRead(USER_ID, false)).thenReturn(5L);

            long count = notificationService.getUnreadCount(USER_ID);

            assertThat(count).isEqualTo(5L);
            verify(notificationRepository).countByUserIdAndIsRead(USER_ID, false);
        }

        @Test
        @DisplayName("無未讀時回傳 0")
        void whenNoUnread_returnsZero() {
            when(notificationRepository.countByUserIdAndIsRead(USER_ID, false)).thenReturn(0L);

            long count = notificationService.getUnreadCount(USER_ID);

            assertThat(count).isZero();
        }
    }

    private static Notification notification(Long userId, boolean isRead) {
        User u = new User();
        u.setId(userId);
        Notification n = Notification.builder()
                .user(u)
                .type(NotificationType.TASK_COMPLETED)
                .title("Task Done")
                .content("Task completed")
                .isRead(isRead)
                .createdAt(LocalDateTime.now())
                .build();
        return n;
    }
}
