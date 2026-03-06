package com.workreport.unit.service;

import com.workreport.dto.common.PageResponse;
import com.workreport.dto.hoursrequest.HoursRequestResponse;
import com.workreport.dto.hoursrequest.ReviewHoursRequestRequest;
import com.workreport.entity.HoursRequest;
import com.workreport.entity.Project;
import com.workreport.entity.Task;
import com.workreport.entity.User;
import com.workreport.enums.HoursRequestStatus;
import com.workreport.enums.HoursRequestTargetType;
import com.workreport.enums.NotificationType;
import com.workreport.exception.BusinessRuleException;
import com.workreport.exception.ResourceNotFoundException;
import com.workreport.repository.HoursRequestRepository;
import com.workreport.repository.ProjectRepository;
import com.workreport.repository.TaskRepository;
import com.workreport.repository.UserRepository;
import com.workreport.service.HoursRequestReviewService;
import com.workreport.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
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
    private HoursRequestReviewService service;

    private static final Long REQUEST_ID = 1L;
    private static final Long ADMIN_USER_ID = 10L;
    private static final Long REQUESTER_ID = 20L;
    private static final Long PROJECT_ID = 100L;
    private static final Long TASK_ID = 200L;
    private static final LocalDateTime NOW = LocalDateTime.of(2025, 3, 6, 12, 0);

    private User reviewer;
    private User requester;
    private Project project;
    private Task task;
    private HoursRequest hoursRequest;

    @BeforeEach
    void setUp() {
        reviewer = new User();
        reviewer.setId(ADMIN_USER_ID);
        reviewer.setName("Admin");

        requester = new User();
        requester.setId(REQUESTER_ID);
        requester.setName("Requester");

        project = new Project();
        project.setId(PROJECT_ID);
        project.setName("Test Project");
        project.setTotalBudgetHours(BigDecimal.valueOf(100));

        task = new Task();
        task.setId(TASK_ID);
        task.setName("Test Task");
        task.setProject(project);
        task.setBudgetHours(BigDecimal.valueOf(20));

        hoursRequest = new HoursRequest();
        hoursRequest.setId(REQUEST_ID);
        hoursRequest.setProject(project);
        hoursRequest.setRequester(requester);
        hoursRequest.setRequestedHours(BigDecimal.valueOf(10));
        hoursRequest.setDescription("Need more hours");
        hoursRequest.setTargetType(HoursRequestTargetType.PROJECT);
        hoursRequest.setTargetTask(null);
        hoursRequest.setStatus(HoursRequestStatus.PENDING);
        hoursRequest.setCreatedAt(NOW);
        hoursRequest.setUpdatedAt(NOW);
    }

    @Nested
    @DisplayName("reviewRequest")
    class ReviewRequest {

        @Test
        @DisplayName("decision 非法（非 APPROVED/REJECTED）時拋出 BusinessRuleException")
        void invalidDecision_throwsBusinessRuleException() {
            ReviewHoursRequestRequest request = new ReviewHoursRequestRequest(
                    HoursRequestStatus.PENDING, "note");

            assertThatThrownBy(() -> service.reviewRequest(ADMIN_USER_ID, REQUEST_ID, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("decision 只能為 APPROVED 或 REJECTED");

            verify(hoursRequestRepository, never()).save(any());
            verify(notificationService, never()).notify(any(), any(), any(), any());
        }

        @Test
        @DisplayName("HoursRequest 不存在時拋出 ResourceNotFoundException")
        void requestNotFound_throwsResourceNotFoundException() {
            ReviewHoursRequestRequest request = new ReviewHoursRequestRequest(
                    HoursRequestStatus.APPROVED, "ok");
            when(hoursRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.reviewRequest(ADMIN_USER_ID, REQUEST_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("HoursRequest not found");

            verify(userRepository, never()).findById(any());
            verify(notificationService, never()).notify(any(), any(), any(), any());
        }

        @Test
        @DisplayName("已非 PENDING 時拋出 BusinessRuleException")
        void notPending_throwsBusinessRuleException() {
            hoursRequest.setStatus(HoursRequestStatus.APPROVED);
            ReviewHoursRequestRequest request = new ReviewHoursRequestRequest(
                    HoursRequestStatus.REJECTED, "reject");
            when(hoursRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(hoursRequest));

            assertThatThrownBy(() -> service.reviewRequest(ADMIN_USER_ID, REQUEST_ID, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("此申請已被審核");

            verify(hoursRequestRepository, never()).save(any());
            verify(notificationService, never()).notify(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Admin User 不存在時拋出 ResourceNotFoundException")
        void reviewerNotFound_throwsResourceNotFoundException() {
            ReviewHoursRequestRequest request = new ReviewHoursRequestRequest(
                    HoursRequestStatus.APPROVED, "ok");
            when(hoursRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(hoursRequest));
            when(userRepository.findById(ADMIN_USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.reviewRequest(ADMIN_USER_ID, REQUEST_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(projectRepository, never()).save(any());
            verify(notificationService, never()).notify(any(), any(), any(), any());
        }

        @Test
        @DisplayName("核准 PROJECT：增加 project totalBudgetHours 並 notify requester")
        void approveProject_increasesBudgetAndNotifies() {
            ReviewHoursRequestRequest request = new ReviewHoursRequestRequest(
                    HoursRequestStatus.APPROVED, "Approved");
            when(hoursRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(hoursRequest));
            when(userRepository.findById(ADMIN_USER_ID)).thenReturn(Optional.of(reviewer));
            when(hoursRequestRepository.save(any(HoursRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            HoursRequestResponse response = service.reviewRequest(ADMIN_USER_ID, REQUEST_ID, request);

            assertThat(response.status()).isEqualTo(HoursRequestStatus.APPROVED);
            assertThat(response.reviewerId()).isEqualTo(ADMIN_USER_ID);
            assertThat(response.reviewComment()).isEqualTo("Approved");
            assertThat(project.getTotalBudgetHours()).isEqualByComparingTo(BigDecimal.valueOf(110));

            verify(projectRepository).save(project);
            verify(taskRepository, never()).save(any());
            verify(notificationService).notify(
                    eq(REQUESTER_ID),
                    eq(NotificationType.HOURS_REQUEST_APPROVED),
                    eq("時數增補申請已核准"),
                    eq("您的專案 'Test Project' 時數增補申請已核准"));
            verify(hoursRequestRepository).save(hoursRequest);
        }

        @Test
        @DisplayName("核准 TASK：增加 task budgetHours 並 notify requester")
        void approveTask_increasesBudgetAndNotifies() {
            hoursRequest.setTargetType(HoursRequestTargetType.TASK);
            hoursRequest.setTargetTask(task);
            ReviewHoursRequestRequest request = new ReviewHoursRequestRequest(
                    HoursRequestStatus.APPROVED, "OK");
            when(hoursRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(hoursRequest));
            when(userRepository.findById(ADMIN_USER_ID)).thenReturn(Optional.of(reviewer));
            when(hoursRequestRepository.save(any(HoursRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            HoursRequestResponse response = service.reviewRequest(ADMIN_USER_ID, REQUEST_ID, request);

            assertThat(response.status()).isEqualTo(HoursRequestStatus.APPROVED);
            assertThat(task.getBudgetHours()).isEqualByComparingTo(BigDecimal.valueOf(30));

            verify(taskRepository).save(task);
            verify(projectRepository, never()).save(any());
            verify(notificationService).notify(
                    eq(REQUESTER_ID),
                    eq(NotificationType.HOURS_REQUEST_APPROVED),
                    eq("時數增補申請已核准"),
                    eq("您的專案 'Test Project' 時數增補申請已核准"));
        }

        @Test
        @DisplayName("駁回：只更新狀態並 notify REJECTED")
        void reject_updatesStatusAndNotifies() {
            ReviewHoursRequestRequest request = new ReviewHoursRequestRequest(
                    HoursRequestStatus.REJECTED, "Rejected");
            when(hoursRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(hoursRequest));
            when(userRepository.findById(ADMIN_USER_ID)).thenReturn(Optional.of(reviewer));
            when(hoursRequestRepository.save(any(HoursRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            HoursRequestResponse response = service.reviewRequest(ADMIN_USER_ID, REQUEST_ID, request);

            assertThat(response.status()).isEqualTo(HoursRequestStatus.REJECTED);
            assertThat(response.reviewComment()).isEqualTo("Rejected");
            assertThat(project.getTotalBudgetHours()).isEqualByComparingTo(BigDecimal.valueOf(100));

            verify(projectRepository, never()).save(any());
            verify(taskRepository, never()).save(any());
            verify(notificationService).notify(
                    eq(REQUESTER_ID),
                    eq(NotificationType.HOURS_REQUEST_REJECTED),
                    eq("時數增補申請已駁回"),
                    eq("您的專案 'Test Project' 時數增補申請已駁回"));
            verify(hoursRequestRepository).save(hoursRequest);
        }
    }

    @Nested
    @DisplayName("getAllRequests")
    class GetAllRequests {

        @Test
        @DisplayName("分頁查詢並轉成 PageResponse")
        void returnsPagedResponse() {
            Pageable pageable = PageRequest.of(0, 10);
            when(hoursRequestRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(hoursRequest), pageable, 1));

            PageResponse<HoursRequestResponse> result = service.getAllRequests(pageable);

            assertThat(result.content()).hasSize(1);
            assertThat(result.page()).isZero();
            assertThat(result.size()).isEqualTo(10);
            assertThat(result.totalElements()).isOne();
            assertThat(result.totalPages()).isOne();

            HoursRequestResponse first = result.content().get(0);
            assertThat(first.id()).isEqualTo(REQUEST_ID);
            assertThat(first.projectId()).isEqualTo(PROJECT_ID);
            assertThat(first.projectName()).isEqualTo("Test Project");
            assertThat(first.requesterId()).isEqualTo(REQUESTER_ID);
            assertThat(first.requesterName()).isEqualTo("Requester");
            assertThat(first.requestedHours()).isEqualByComparingTo(BigDecimal.valueOf(10));
            assertThat(first.description()).isEqualTo("Need more hours");
            assertThat(first.targetType()).isEqualTo(HoursRequestTargetType.PROJECT);
            assertThat(first.targetTaskId()).isNull();
            assertThat(first.targetTaskName()).isNull();
            assertThat(first.status()).isEqualTo(HoursRequestStatus.PENDING);
            assertThat(first.reviewerId()).isNull();
            assertThat(first.reviewerName()).isNull();
            assertThat(first.reviewComment()).isNull();
            assertThat(first.reviewedAt()).isNull();
        }

        @Test
        @DisplayName("空頁時回傳空 content")
        void emptyPage_returnsEmptyContent() {
            Pageable pageable = PageRequest.of(0, 10);
            when(hoursRequestRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            PageResponse<HoursRequestResponse> result = service.getAllRequests(pageable);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
            assertThat(result.totalPages()).isZero();
        }
    }
}
