package com.workreport.unit.service;

import com.workreport.dto.task.CreateTaskRequest;
import com.workreport.dto.task.TaskResponse;
import com.workreport.dto.task.UpdateTaskRequest;
import com.workreport.entity.Project;
import com.workreport.entity.Task;
import com.workreport.entity.User;
import com.workreport.enums.AuditActionType;
import com.workreport.enums.ProjectStatus;
import com.workreport.enums.Role;
import com.workreport.enums.TaskStatus;
import com.workreport.exception.BusinessRuleException;
import com.workreport.repository.ProjectRepository;
import com.workreport.repository.TaskRepository;
import com.workreport.repository.UserRepository;
import com.workreport.repository.WorkEntryRepository;
import com.workreport.service.AuditLogService;
import com.workreport.service.TaskService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkEntryRepository workEntryRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TaskService taskService;

    private User pmUser;
    private User executor;
    private Project project;
    private Task task;

    @BeforeEach
    void setUp() {
        pmUser = new User();
        pmUser.setId(1L);
        pmUser.setName("PM");
        pmUser.setRoles(Set.of(Role.PM));

        executor = new User();
        executor.setId(2L);
        executor.setName("Executor");
        executor.setRoles(Set.of(Role.EXECUTOR));

        project = new Project();
        project.setId(10L);
        project.setName("Test Project");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setPm(pmUser);
        project.setTotalBudgetHours(new BigDecimal("500.0"));
        project.setConsumedHours(BigDecimal.ZERO);

        task = new Task();
        task.setId(100L);
        task.setName("Test Task");
        task.setProject(project);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setBudgetHours(new BigDecimal("40.0"));
        task.setConsumedHours(new BigDecimal("10.0"));
        task.setAssignee(executor);
        task.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createTask_success() {
        CreateTaskRequest request = new CreateTaskRequest("New Task", new BigDecimal("20.0"), 2L);

        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(2L)).thenReturn(Optional.of(executor));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(200L);
            t.setCreatedAt(LocalDateTime.now());
            return t;
        });

        TaskResponse response = taskService.createTask(1L, 10L, request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("New Task");
        assertThat(response.status()).isEqualTo(TaskStatus.PENDING);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_closedProject_throwsException() {
        project.setStatus(ProjectStatus.CLOSED);
        CreateTaskRequest request = new CreateTaskRequest("New Task", new BigDecimal("20.0"), null);

        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> taskService.createTask(1L, 10L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("專案已關閉");
    }

    @Test
    void updateTask_success() {
        UpdateTaskRequest request = new UpdateTaskRequest("Updated Task", new BigDecimal("50.0"), 2L);

        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(executor));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskResponse response = taskService.updateTask(1L, 10L, 100L, request);

        assertThat(response).isNotNull();
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void closeTask_force_createsAuditLog() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskResponse response = taskService.closeTask(1L, 10L, 100L);

        assertThat(response.status()).isEqualTo(TaskStatus.CLOSED);
        verify(auditLogService).log(eq(AuditActionType.TASK_FORCE_CLOSED), eq(1L),
                eq("Task"), eq(100L), anyString());
    }

    @Test
    void deleteTask_withWorkEntries_throwsException() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(workEntryRepository.existsByTaskId(100L)).thenReturn(true);

        assertThatThrownBy(() -> taskService.deleteTask(1L, 10L, 100L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("工時紀錄");
    }

    @Test
    void deleteTask_withoutWorkEntries_success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(workEntryRepository.existsByTaskId(100L)).thenReturn(false);

        taskService.deleteTask(1L, 10L, 100L);

        verify(taskRepository).delete(task);
    }
}
