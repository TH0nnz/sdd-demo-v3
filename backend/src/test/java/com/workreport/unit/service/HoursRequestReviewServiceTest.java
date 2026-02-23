package com.workreport.unit.service;

import com.workreport.dto.hoursrequest.HoursRequestResponse;
import com.workreport.dto.hoursrequest.ReviewHoursRequestRequest;
import com.workreport.entity.HoursRequest;
import com.workreport.entity.Project;
import com.workreport.entity.Task;
import com.workreport.entity.User;
import com.workreport.enums.HoursRequestStatus;
import com.workreport.enums.HoursRequestTargetType;
import com.workreport.enums.NotificationType;
import com.workreport.enums.Role;
import com.workreport.exception.BusinessRuleException;
import com.workreport.repository.HoursRequestRepository;
import com.workreport.repository.ProjectRepository;
import com.workreport.repository.TaskRepository;
import com.workreport.repository.UserRepository;
import com.workreport.service.HoursRequestReviewService;
import com.workreport.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HoursRequestReviewServiceTest {

    @Mock
    private HoursRequestRepository hoursRequestRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private HoursRequestReviewService hoursRequestReviewService;

    private User adminUser;
    private User pmUser;
    private Project project;
    private Task task;
    private HoursRequest pendingRequest;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setName("Admin");
        adminUser.setRoles(Set.of(Role.ADMIN));

        pmUser = new User();
        pmUser.setId(2L);
        pmUser.setName("PM User");
        pmUser.setRoles(Set.of(Role.PM));

        project = new Project();
        project.setId(100L);
        project.setName("Test Project");
        project.setTotalBudgetHours(new BigDecimal("500.0"));
        project.setConsumedHours(new BigDecimal("100.0"));

        task = new Task();
        task.setId(200L);
        task.setName("Test Task");
        task.setProject(project);
        task.setBudgetHours(new BigDecimal("40.0"));

        pendingRequest = new HoursRequest();
        pendingRequest.setId(1L);
        pendingRequest.setProject(project);
        pendingRequest.setRequester(pmUser);
        pendingRequest.setRequestedHours(new BigDecimal("50.0"));
        pendingRequest.setDescription("需要增補時數");
        pendingRequest.setTargetType(HoursRequestTargetType.PROJECT);
        pendingRequest.setStatus(HoursRequestStatus.PENDING);
        pendingRequest.setCreatedAt(LocalDateTime.now());
        pendingRequest.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void approveRequest_addsHoursToProject() {
        ReviewHoursRequestRequest review = new ReviewHoursRequestRequest(HoursRequestStatus.APPROVED, "核准");

        when(hoursRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(hoursRequestRepository.save(any(HoursRequest.class))).thenReturn(pendingRequest);
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        HoursRequestResponse response = hoursRequestReviewService.reviewRequest(1L, 1L, review);

        assertThat(response).isNotNull();
        assertThat(project.getTotalBudgetHours()).isEqualByComparingTo(new BigDecimal("550.0"));
        verify(notificationService).notify(eq(2L), eq(NotificationType.HOURS_REQUEST_APPROVED),
                anyString(), anyString());
    }

    @Test
    void approveRequest_taskTarget_addsHoursToTask() {
        pendingRequest.setTargetType(HoursRequestTargetType.TASK);
        pendingRequest.setTargetTask(task);
        pendingRequest.setRequestedHours(new BigDecimal("20.0"));

        ReviewHoursRequestRequest review = new ReviewHoursRequestRequest(HoursRequestStatus.APPROVED, "核准 task 增時");

        when(hoursRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(hoursRequestRepository.save(any(HoursRequest.class))).thenReturn(pendingRequest);
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        hoursRequestReviewService.reviewRequest(1L, 1L, review);

        assertThat(task.getBudgetHours()).isEqualByComparingTo(new BigDecimal("60.0"));
    }

    @Test
    void rejectRequest_success() {
        ReviewHoursRequestRequest review = new ReviewHoursRequestRequest(HoursRequestStatus.REJECTED, "預算不足");

        when(hoursRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(hoursRequestRepository.save(any(HoursRequest.class))).thenReturn(pendingRequest);

        HoursRequestResponse response = hoursRequestReviewService.reviewRequest(1L, 1L, review);

        assertThat(response).isNotNull();
        verify(notificationService).notify(eq(2L), eq(NotificationType.HOURS_REQUEST_REJECTED),
                anyString(), anyString());
        verify(projectRepository, never()).save(any());
    }

    @Test
    void reviewAlreadyProcessedRequest_rejected() {
        pendingRequest.setStatus(HoursRequestStatus.APPROVED);

        ReviewHoursRequestRequest review = new ReviewHoursRequestRequest(HoursRequestStatus.REJECTED, "改為駁回");

        when(hoursRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> hoursRequestReviewService.reviewRequest(1L, 1L, review))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("已被審核");
    }
}
