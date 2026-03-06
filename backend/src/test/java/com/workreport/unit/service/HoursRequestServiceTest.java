package com.workreport.unit.service;

import com.workreport.dto.common.PageResponse;
import com.workreport.dto.hoursrequest.CreateHoursRequestRequest;
import com.workreport.dto.hoursrequest.HoursRequestResponse;
import com.workreport.entity.HoursRequest;
import com.workreport.entity.Project;
import com.workreport.entity.Task;
import com.workreport.entity.User;
import com.workreport.enums.HoursRequestStatus;
import com.workreport.enums.HoursRequestTargetType;
import com.workreport.enums.NotificationType;
import com.workreport.enums.Role;
import com.workreport.exception.BusinessRuleException;
import com.workreport.exception.ResourceNotFoundException;
import com.workreport.repository.HoursRequestRepository;
import com.workreport.repository.ProjectRepository;
import com.workreport.repository.TaskRepository;
import com.workreport.repository.UserRepository;
import com.workreport.service.HoursRequestService;
import com.workreport.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HoursRequestService")
class HoursRequestServiceTest {

    private static final Long PM_USER_ID = 1L;
    private static final Long PROJECT_ID = 10L;
    private static final Long TASK_ID = 20L;
    private static final Long HOURS_REQUEST_ID = 100L;

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
    private HoursRequestService hoursRequestService;

    private User pmUser;
    private Project project;
    private Task task;
    private User admin1;
    private User admin2;

    @BeforeEach
    void setUp() {
        pmUser = new User();
        pmUser.setId(PM_USER_ID);
        pmUser.setName("PM User");

        project = new Project();
        project.setId(PROJECT_ID);
        project.setName("Test Project");
        project.setPm(pmUser);

        task = new Task();
        task.setId(TASK_ID);
        task.setName("Task 1");
        task.setProject(project);

        admin1 = new User();
        admin1.setId(2L);
        admin1.setName("Admin1");

        admin2 = new User();
        admin2.setId(3L);
        admin2.setName("Admin2");
    }

    @Nested
    @DisplayName("createRequest")
    class CreateRequest {

        @Test
        @DisplayName("project 不存在時拋出 ResourceNotFoundException")
        void projectNotFound() {
            CreateHoursRequestRequest request = new CreateHoursRequestRequest(
                    PROJECT_ID,
                    new BigDecimal("8.0"),
                    "need more hours",
                    HoursRequestTargetType.PROJECT,
                    null
            );
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> hoursRequestService.createRequest(PM_USER_ID, request));
            assertEquals("Project not found: " + PROJECT_ID, ex.getMessage());
            verify(hoursRequestRepository, never()).save(any());
            verify(notificationService, never()).notify(any(), any(), any(), any());
        }

        @Test
        @DisplayName("非 PM 時拋出 BusinessRuleException")
        void notPm() {
            User otherPm = new User();
            otherPm.setId(999L);
            project.setPm(otherPm);

            CreateHoursRequestRequest request = new CreateHoursRequestRequest(
                    PROJECT_ID,
                    new BigDecimal("8.0"),
                    "need more hours",
                    HoursRequestTargetType.PROJECT,
                    null
            );
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> hoursRequestService.createRequest(PM_USER_ID, request));
            assertEquals("此專案非您負責", ex.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verify(hoursRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("targetType 為 TASK 時未指定 targetTaskId 拋出 BusinessRuleException")
        void taskTypeWithoutTargetTaskId() {
            CreateHoursRequestRequest request = new CreateHoursRequestRequest(
                    PROJECT_ID,
                    new BigDecimal("8.0"),
                    "need more hours",
                    HoursRequestTargetType.TASK,
                    null
            );
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> hoursRequestService.createRequest(PM_USER_ID, request));
            assertEquals("targetType 為 TASK 時必須指定 targetTaskId", ex.getMessage());
            verify(taskRepository, never()).findById(any());
            verify(hoursRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("targetType 為 TASK 時 task 不屬於此專案拋出 BusinessRuleException")
        void taskNotInProject() {
            Project otherProject = new Project();
            otherProject.setId(999L);
            task.setProject(otherProject);

            CreateHoursRequestRequest request = new CreateHoursRequestRequest(
                    PROJECT_ID,
                    new BigDecimal("8.0"),
                    "need more hours",
                    HoursRequestTargetType.TASK,
                    TASK_ID
            );
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> hoursRequestService.createRequest(PM_USER_ID, request));
            assertEquals("指定的 Task 不屬於此專案", ex.getMessage());
            verify(hoursRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("targetType 為 TASK 時 task 不存在拋出 ResourceNotFoundException")
        void taskNotFound() {
            CreateHoursRequestRequest request = new CreateHoursRequestRequest(
                    PROJECT_ID,
                    new BigDecimal("8.0"),
                    "need more hours",
                    HoursRequestTargetType.TASK,
                    TASK_ID
            );
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> hoursRequestService.createRequest(PM_USER_ID, request));
            assertEquals("Task not found: " + TASK_ID, ex.getMessage());
        }

        @Test
        @DisplayName("成功建立 PROJECT 類型申請並 notify 所有 ADMIN")
        void successProjectType() {
            CreateHoursRequestRequest request = new CreateHoursRequestRequest(
                    PROJECT_ID,
                    new BigDecimal("8.0"),
                    "need more hours",
                    HoursRequestTargetType.PROJECT,
                    null
            );
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(userRepository.findById(PM_USER_ID)).thenReturn(Optional.of(pmUser));
            when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin1, admin2));

            HoursRequest saved = new HoursRequest();
            saved.setId(HOURS_REQUEST_ID);
            saved.setProject(project);
            saved.setRequester(pmUser);
            saved.setRequestedHours(request.requestedHours());
            saved.setDescription(request.description());
            saved.setTargetType(HoursRequestTargetType.PROJECT);
            saved.setTargetTask(null);
            saved.setStatus(HoursRequestStatus.PENDING);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());

            when(hoursRequestRepository.save(any(HoursRequest.class))).thenAnswer(inv -> {
                HoursRequest hr = inv.getArgument(0);
                hr.setId(HOURS_REQUEST_ID);
                return hr;
            });

            HoursRequestResponse response = hoursRequestService.createRequest(PM_USER_ID, request);

            assertNotNull(response);
            assertEquals(HOURS_REQUEST_ID, response.id());
            assertEquals(PROJECT_ID, response.projectId());
            assertEquals("Test Project", response.projectName());
            assertEquals(PM_USER_ID, response.requesterId());
            assertEquals("PM User", response.requesterName());
            assertEquals(new BigDecimal("8.0"), response.requestedHours());
            assertEquals("need more hours", response.description());
            assertEquals(HoursRequestTargetType.PROJECT, response.targetType());
            assertNull(response.targetTaskId());
            assertNull(response.targetTaskName());
            assertEquals(HoursRequestStatus.PENDING, response.status());

            verify(taskRepository, never()).findById(any());
            verify(hoursRequestRepository, times(1)).save(any(HoursRequest.class));

            verify(notificationService, times(2)).notify(any(Long.class), eq(NotificationType.HOURS_REQUEST_SUBMITTED),
                    eq("新時數增補申請"), any(String.class));
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(notificationService, times(2)).notify(any(), any(), any(), bodyCaptor.capture());
            assertEquals("PM PM User 提交了專案 'Test Project' 的時數增補申請", bodyCaptor.getValue());
        }

        @Test
        @DisplayName("成功建立 TASK 類型申請並 notify 所有 ADMIN")
        void successTaskTypeAndNotifyAdmins() {
            CreateHoursRequestRequest request = new CreateHoursRequestRequest(
                    PROJECT_ID,
                    new BigDecimal("5.0"),
                    "task overflow",
                    HoursRequestTargetType.TASK,
                    TASK_ID
            );
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            when(userRepository.findById(PM_USER_ID)).thenReturn(Optional.of(pmUser));
            when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin1, admin2));

            when(hoursRequestRepository.save(any(HoursRequest.class))).thenAnswer(inv -> {
                HoursRequest hr = inv.getArgument(0);
                hr.setId(HOURS_REQUEST_ID);
                return hr;
            });

            HoursRequestResponse response = hoursRequestService.createRequest(PM_USER_ID, request);

            assertNotNull(response);
            assertEquals(HOURS_REQUEST_ID, response.id());
            assertEquals(PROJECT_ID, response.projectId());
            assertEquals(TASK_ID, response.targetTaskId());
            assertEquals("Task 1", response.targetTaskName());
            assertEquals(HoursRequestTargetType.TASK, response.targetType());
            assertEquals(new BigDecimal("5.0"), response.requestedHours());

            ArgumentCaptor<HoursRequest> saveCaptor = ArgumentCaptor.forClass(HoursRequest.class);
            verify(hoursRequestRepository).save(saveCaptor.capture());
            assertEquals(task, saveCaptor.getValue().getTargetTask());

            verify(notificationService, times(2)).notify(any(Long.class), eq(NotificationType.HOURS_REQUEST_SUBMITTED),
                    eq("新時數增補申請"), any(String.class));
        }
    }

    @Nested
    @DisplayName("getRequests")
    class GetRequests {

        @Test
        @DisplayName("有資料時回傳分頁結果")
        void withData() {
            Pageable pageable = PageRequest.of(0, 10);
            HoursRequest hr = new HoursRequest();
            hr.setId(HOURS_REQUEST_ID);
            hr.setProject(project);
            hr.setRequester(pmUser);
            hr.setRequestedHours(new BigDecimal("8.0"));
            hr.setDescription("desc");
            hr.setTargetType(HoursRequestTargetType.PROJECT);
            hr.setTargetTask(null);
            hr.setStatus(HoursRequestStatus.PENDING);
            hr.setReviewer(null);
            hr.setReviewComment(null);
            hr.setReviewedAt(null);
            hr.setCreatedAt(LocalDateTime.now());
            hr.setUpdatedAt(LocalDateTime.now());

            when(hoursRequestRepository.findByRequesterId(PM_USER_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(hr), pageable, 1));

            PageResponse<HoursRequestResponse> result = hoursRequestService.getRequests(PM_USER_ID, pageable);

            assertNotNull(result);
            assertEquals(1, result.content().size());
            assertEquals(0, result.page());
            assertEquals(10, result.size());
            assertEquals(1, result.totalElements());
            assertEquals(1, result.totalPages());

            HoursRequestResponse first = result.content().get(0);
            assertEquals(HOURS_REQUEST_ID, first.id());
            assertEquals(PROJECT_ID, first.projectId());
            assertEquals("Test Project", first.projectName());
            assertEquals(PM_USER_ID, first.requesterId());
            assertEquals("PM User", first.requesterName());
            assertEquals(HoursRequestStatus.PENDING, first.status());
        }
    }
}
