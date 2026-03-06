package com.workreport.unit.service;

import com.workreport.dto.common.PageResponse;
import com.workreport.dto.task.TaskResponse;
import com.workreport.entity.Project;
import com.workreport.entity.Task;
import com.workreport.entity.User;
import com.workreport.enums.NotificationType;
import com.workreport.enums.TaskStatus;
import com.workreport.exception.BusinessRuleException;
import com.workreport.exception.ResourceNotFoundException;
import com.workreport.repository.TaskRepository;
import com.workreport.service.ExecutorTaskService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutorTaskService")
class ExecutorTaskServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long TASK_ID = 10L;
    private static final Long PM_ID = 2L;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ExecutorTaskService executorTaskService;

    private User assignee;
    private User pm;
    private Project project;
    private Task task;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        assignee = new User();
        assignee.setId(USER_ID);
        assignee.setName("Assignee");

        pm = new User();
        pm.setId(PM_ID);
        pm.setName("PM");

        project = new Project();
        project.setId(20L);
        project.setName("Project A");
        project.setPm(pm);

        task = new Task();
        task.setId(TASK_ID);
        task.setName("Task 1");
        task.setProject(project);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setBudgetHours(new BigDecimal("10.0"));
        task.setConsumedHours(new BigDecimal("2.0"));
        task.setAssignee(assignee);
        task.setCreatedAt(LocalDateTime.now());
        task.setVersion(1L);

        pageable = PageRequest.of(0, 10);
    }

    @Nested
    @DisplayName("getMyTasks")
    class GetMyTasks {

        @Test
        @DisplayName("有 statusFilter 時呼叫 findByAssigneeIdAndStatusIn 並回傳 PageResponse")
        void withStatusFilter_callsFindByAssigneeIdAndStatusIn_returnsPageResponse() {
            List<Task> content = List.of(task);
            PageImpl<Task> page = new PageImpl<>(content, pageable, 1);

            when(taskRepository.findByAssigneeIdAndStatusIn(eq(USER_ID), eq(List.of(TaskStatus.IN_PROGRESS)), eq(pageable)))
                    .thenReturn(page);

            PageResponse<TaskResponse> result = executorTaskService.getMyTasks(USER_ID, TaskStatus.IN_PROGRESS, pageable);

            verify(taskRepository).findByAssigneeIdAndStatusIn(USER_ID, List.of(TaskStatus.IN_PROGRESS), pageable);
            verify(taskRepository, never()).findByAssigneeId(any(), any());

            assertNotNull(result);
            assertEquals(1, result.content().size());
            assertEquals(0, result.page());
            assertEquals(10, result.size());
            assertEquals(1, result.totalElements());
            assertEquals(1, result.totalPages());

            TaskResponse first = result.content().get(0);
            assertEquals(TASK_ID, first.id());
            assertEquals("Task 1", first.name());
            assertEquals(20L, first.projectId());
            assertEquals("Project A", first.projectName());
            assertEquals(TaskStatus.IN_PROGRESS, first.status());
            assertEquals(USER_ID, first.assigneeId());
            assertEquals("Assignee", first.assigneeName());
        }

        @Test
        @DisplayName("無 statusFilter 時呼叫 findByAssigneeId 並回傳 PageResponse")
        void withoutStatusFilter_callsFindByAssigneeId_returnsPageResponse() {
            List<Task> content = List.of(task);
            PageImpl<Task> page = new PageImpl<>(content, pageable, 1);

            when(taskRepository.findByAssigneeId(eq(USER_ID), eq(pageable))).thenReturn(page);

            PageResponse<TaskResponse> result = executorTaskService.getMyTasks(USER_ID, null, pageable);

            verify(taskRepository).findByAssigneeId(USER_ID, pageable);
            verify(taskRepository, never()).findByAssigneeIdAndStatusIn(any(), any(), any());

            assertNotNull(result);
            assertEquals(1, result.content().size());
            assertEquals(TASK_ID, result.content().get(0).id());
        }
    }

    @Nested
    @DisplayName("completeTask")
    class CompleteTask {

        @Test
        @DisplayName("task 不存在時拋出 ResourceNotFoundException")
        void taskNotFound_throwsResourceNotFoundException() {
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> executorTaskService.completeTask(USER_ID, TASK_ID));

            verify(taskRepository).findById(TASK_ID);
            verify(taskRepository, never()).save(any());
            verify(notificationService, never()).notify(any(), any(), any(), any());
        }

        @Test
        @DisplayName("task 的 assignee 為 null 時拋出 BusinessRuleException FORBIDDEN")
        void taskAssigneeNull_throwsBusinessRuleExceptionForbidden() {
            task.setAssignee(null);
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> executorTaskService.completeTask(USER_ID, TASK_ID));

            assertEquals("Task is not assigned to you", ex.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verify(taskRepository, never()).save(any());
            verify(notificationService, never()).notify(any(), any(), any(), any());
        }

        @Test
        @DisplayName("task 的 assignee 非本人時拋出 BusinessRuleException FORBIDDEN")
        void taskAssigneeNotCurrentUser_throwsBusinessRuleExceptionForbidden() {
            User otherUser = new User();
            otherUser.setId(999L);
            otherUser.setName("Other");
            task.setAssignee(otherUser);
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> executorTaskService.completeTask(USER_ID, TASK_ID));

            assertEquals("Task is not assigned to you", ex.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verify(taskRepository, never()).save(any());
            verify(notificationService, never()).notify(any(), any(), any(), any());
        }

        @Test
        @DisplayName("task 狀態非 IN_PROGRESS 時拋出 BusinessRuleException")
        void taskStatusNotInProgress_throwsBusinessRuleException() {
            task.setStatus(TaskStatus.PENDING);
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> executorTaskService.completeTask(USER_ID, TASK_ID));

            assertEquals("Only IN_PROGRESS tasks can be completed", ex.getMessage());
            verify(taskRepository, never()).save(any());
            verify(notificationService, never()).notify(any(), any(), any(), any());
        }

        @Test
        @DisplayName("成功完成任務並通知 PM")
        void success_setsCompleted_saves_notifiesPm_returnsTaskResponse() {
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskResponse result = executorTaskService.completeTask(USER_ID, TASK_ID);

            assertEquals(TaskStatus.COMPLETED, task.getStatus());
            verify(taskRepository).save(task);

            ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<NotificationType> typeCaptor = ArgumentCaptor.forClass(NotificationType.class);
            ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
            verify(notificationService).notify(userIdCaptor.capture(), typeCaptor.capture(),
                    titleCaptor.capture(), contentCaptor.capture());

            assertEquals(PM_ID, userIdCaptor.getValue());
            assertEquals(NotificationType.TASK_COMPLETED, typeCaptor.getValue());
            assertEquals("Task completed", titleCaptor.getValue());
            assertEquals("Task 'Task 1' in project 'Project A' has been marked as completed.", contentCaptor.getValue());

            assertNotNull(result);
            assertEquals(TASK_ID, result.id());
            assertEquals("Task 1", result.name());
            assertEquals(TaskStatus.COMPLETED, result.status());
            assertEquals(USER_ID, result.assigneeId());
        }
    }
}
