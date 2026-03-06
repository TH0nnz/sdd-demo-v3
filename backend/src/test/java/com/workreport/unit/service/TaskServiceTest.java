package com.workreport.unit.service;

import com.workreport.dto.common.PageResponse;
import com.workreport.dto.task.CreateTaskRequest;
import com.workreport.dto.task.TaskResponse;
import com.workreport.dto.task.UpdateTaskRequest;
import com.workreport.dto.user.UserResponse;
import com.workreport.entity.Department;
import com.workreport.entity.Project;
import com.workreport.entity.Task;
import com.workreport.entity.User;
import com.workreport.enums.AuditActionType;
import com.workreport.enums.ProjectStatus;
import com.workreport.enums.Role;
import com.workreport.enums.TaskStatus;
import com.workreport.exception.BusinessRuleException;
import com.workreport.exception.ResourceNotFoundException;
import com.workreport.repository.ProjectRepository;
import com.workreport.repository.TaskRepository;
import com.workreport.repository.UserRepository;
import com.workreport.repository.WorkEntryRepository;
import com.workreport.service.AuditLogService;
import com.workreport.service.TaskService;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    private static final Long PM_ID = 10L;
    private static final Long PROJECT_ID = 1L;
    private static final Long TASK_ID = 100L;
    private static final Long DEPT_ID = 20L;
    private static final LocalDateTime NOW = LocalDateTime.of(2025, 3, 1, 12, 0);

    private User pmUser;
    private User executorUser;
    private User nonExecutorUser;
    private Department department;
    private Project project;
    private Task task;

    @BeforeEach
    void setUp() {
        pmUser = new User();
        pmUser.setId(PM_ID);
        pmUser.setName("PM User");
        pmUser.setRoles(Set.of(Role.PM));

        department = new Department();
        department.setId(DEPT_ID);
        department.setName("IT");

        executorUser = new User();
        executorUser.setId(2L);
        executorUser.setName("Executor");
        executorUser.setEmail("exec@test.com");
        executorUser.setDepartment(department);
        executorUser.setRoles(Set.of(Role.EXECUTOR));
        executorUser.setActive(true);
        executorUser.setCreatedAt(NOW);

        nonExecutorUser = new User();
        nonExecutorUser.setId(3L);
        nonExecutorUser.setName("Non Executor");
        nonExecutorUser.setRoles(Set.of(Role.PM));

        project = new Project();
        project.setId(PROJECT_ID);
        project.setName("Test Project");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setTotalBudgetHours(BigDecimal.valueOf(100));
        project.setConsumedHours(BigDecimal.ZERO);
        project.setPm(pmUser);
        project.setDepartment(department);

        task = new Task();
        task.setId(TASK_ID);
        task.setName("Task 1");
        task.setProject(project);
        task.setStatus(TaskStatus.PENDING);
        task.setBudgetHours(BigDecimal.valueOf(10));
        task.setConsumedHours(BigDecimal.ZERO);
        task.setAssignee(executorUser);
        task.setCreatedAt(NOW);
    }

    @Nested
    @DisplayName("createTask")
    class CreateTask {

        @Test
        @DisplayName("專案非 PM 負責時拋出 BusinessRuleException")
        void createTask_projectNotOwnedByPm_throws() {
            Project otherPmProject = new Project();
            otherPmProject.setId(PROJECT_ID);
            otherPmProject.setPm(nonExecutorUser);
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(otherPmProject));

            CreateTaskRequest request = new CreateTaskRequest("Task", BigDecimal.TEN, null);

            assertThatThrownBy(() -> taskService.createTask(PM_ID, PROJECT_ID, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("此專案非您負責");
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("專案已關閉時拋出 BusinessRuleException")
        void createTask_projectClosed_throws() {
            project.setStatus(ProjectStatus.CLOSED);
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

            CreateTaskRequest request = new CreateTaskRequest("Task", BigDecimal.TEN, null);

            assertThatThrownBy(() -> taskService.createTask(PM_ID, PROJECT_ID, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("專案已關閉");
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("指定 assignee 但無 EXECUTOR 角色時拋出 BusinessRuleException")
        void createTask_assigneeNotExecutor_throws() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(userRepository.findById(nonExecutorUser.getId())).thenReturn(Optional.of(nonExecutorUser));

            CreateTaskRequest request = new CreateTaskRequest("Task", BigDecimal.TEN, nonExecutorUser.getId());

            assertThatThrownBy(() -> taskService.createTask(PM_ID, PROJECT_ID, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("EXECUTOR 角色");
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("成功建立 Task（無 assignee）並回傳 PENDING")
        void createTask_successWithoutAssignee() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            Task saved = new Task();
            saved.setId(TASK_ID);
            saved.setName("New Task");
            saved.setProject(project);
            saved.setStatus(TaskStatus.PENDING);
            saved.setBudgetHours(BigDecimal.valueOf(8));
            saved.setConsumedHours(BigDecimal.ZERO);
            saved.setAssignee(null);
            saved.setCreatedAt(NOW);
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
                Task t = inv.getArgument(0);
                t.setId(TASK_ID);
                t.setCreatedAt(NOW);
                return t;
            });

            CreateTaskRequest request = new CreateTaskRequest("New Task", BigDecimal.valueOf(8), null);
            TaskResponse response = taskService.createTask(PM_ID, PROJECT_ID, request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(TASK_ID);
            assertThat(response.name()).isEqualTo("New Task");
            assertThat(response.status()).isEqualTo(TaskStatus.PENDING);
            assertThat(response.assigneeId()).isNull();
            assertThat(response.budgetHours()).isEqualByComparingTo(BigDecimal.valueOf(8));

            ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository).save(captor.capture());
            Task savedTask = captor.getValue();
            assertThat(savedTask.getStatus()).isEqualTo(TaskStatus.PENDING);
            assertThat(savedTask.getAssignee()).isNull();
        }

        @Test
        @DisplayName("成功建立 Task（有 EXECUTOR assignee）")
        void createTask_successWithExecutor() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(userRepository.findById(executorUser.getId())).thenReturn(Optional.of(executorUser));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
                Task t = inv.getArgument(0);
                t.setId(TASK_ID);
                t.setCreatedAt(NOW);
                return t;
            });

            CreateTaskRequest request = new CreateTaskRequest("Task", BigDecimal.TEN, executorUser.getId());
            TaskResponse response = taskService.createTask(PM_ID, PROJECT_ID, request);

            assertThat(response).isNotNull();
            assertThat(response.assigneeId()).isEqualTo(executorUser.getId());
            assertThat(response.assigneeName()).isEqualTo(executorUser.getName());

            ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository).save(captor.capture());
            assertThat(captor.getValue().getAssignee()).isEqualTo(executorUser);
        }
    }

    @Nested
    @DisplayName("updateTask")
    class UpdateTask {

        @Test
        @DisplayName("Task 已為終態時拋出 BusinessRuleException")
        void updateTask_terminalStatus_throws() {
            task.setStatus(TaskStatus.CLOSED);
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));

            UpdateTaskRequest request = new UpdateTaskRequest("Updated", BigDecimal.valueOf(15), null);

            assertThatThrownBy(() -> taskService.updateTask(PM_ID, PROJECT_ID, TASK_ID, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("終態");
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("成功更新 name、budgetHours、assignee")
        void updateTask_success() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            when(userRepository.findById(executorUser.getId())).thenReturn(Optional.of(executorUser));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateTaskRequest request = new UpdateTaskRequest("Updated Name", BigDecimal.valueOf(20), executorUser.getId());
            TaskResponse response = taskService.updateTask(PM_ID, PROJECT_ID, TASK_ID, request);

            assertThat(response.name()).isEqualTo("Updated Name");
            assertThat(response.budgetHours()).isEqualByComparingTo(BigDecimal.valueOf(20));
            assertThat(response.assigneeId()).isEqualTo(executorUser.getId());

            ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository).save(captor.capture());
            Task updated = captor.getValue();
            assertThat(updated.getName()).isEqualTo("Updated Name");
            assertThat(updated.getBudgetHours()).isEqualByComparingTo(BigDecimal.valueOf(20));
            assertThat(updated.getAssignee()).isEqualTo(executorUser);
        }

        @Test
        @DisplayName("成功更新並清空 assignee")
        void updateTask_clearAssignee_success() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateTaskRequest request = new UpdateTaskRequest("Updated", BigDecimal.valueOf(12), null);
            TaskResponse response = taskService.updateTask(PM_ID, PROJECT_ID, TASK_ID, request);

            assertThat(response.assigneeId()).isNull();
            assertThat(response.assigneeName()).isNull();

            ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository).save(captor.capture());
            assertThat(captor.getValue().getAssignee()).isNull();
        }
    }

    @Nested
    @DisplayName("closeTask")
    class CloseTask {

        @Test
        @DisplayName("成功關閉 Task 並寫入 auditLog")
        void closeTask_success_auditLog() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskResponse response = taskService.closeTask(PM_ID, PROJECT_ID, TASK_ID);

            assertThat(response.status()).isEqualTo(TaskStatus.CLOSED);

            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository).save(taskCaptor.capture());
            assertThat(taskCaptor.getValue().getStatus()).isEqualTo(TaskStatus.CLOSED);

            verify(auditLogService).log(
                    eq(AuditActionType.TASK_FORCE_CLOSED),
                    eq(PM_ID),
                    eq("Task"),
                    eq(TASK_ID),
                    org.mockito.ArgumentMatchers.contains("PM force closed task")
            );
        }

        @Test
        @DisplayName("Task 已為終態時拋出 BusinessRuleException")
        void closeTask_terminalStatus_throws() {
            task.setStatus(TaskStatus.COMPLETED);
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> taskService.closeTask(PM_ID, PROJECT_ID, TASK_ID))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("終態");
            verify(auditLogService, never()).log(any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("deleteTask")
    class DeleteTask {

        @Test
        @DisplayName("Task 已有工時紀錄時拋出 BusinessRuleException CONFLICT")
        void deleteTask_hasWorkEntry_throws() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            when(workEntryRepository.existsByTaskId(TASK_ID)).thenReturn(true);

            assertThatThrownBy(() -> taskService.deleteTask(PM_ID, PROJECT_ID, TASK_ID))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(ex -> {
                        BusinessRuleException e = (BusinessRuleException) ex;
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(e.getMessage()).contains("工時紀錄");
                    });
            verify(taskRepository, never()).delete(any());
        }

        @Test
        @DisplayName("成功刪除 Task（無工時）")
        void deleteTask_success() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            when(workEntryRepository.existsByTaskId(TASK_ID)).thenReturn(false);

            taskService.deleteTask(PM_ID, PROJECT_ID, TASK_ID);

            verify(taskRepository).delete(task);
        }
    }

    @Nested
    @DisplayName("getTasks")
    class GetTasks {

        @Test
        @DisplayName("依 projectId 與 pageable 回傳分頁 TaskResponse")
        void getTasks_returnsPage() {
            Pageable pageable = PageRequest.of(0, 10);
            PageImpl<Task> page = new PageImpl<>(List.of(task), pageable, 1);
            when(taskRepository.findByProjectId(PROJECT_ID, pageable)).thenReturn(page);

            PageResponse<TaskResponse> result = taskService.getTasks(PROJECT_ID, pageable);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).id()).isEqualTo(TASK_ID);
            assertThat(result.totalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getAssignableExecutors")
    class GetAssignableExecutors {

        @Test
        @DisplayName("專案無 department 時回傳空列表")
        void getAssignableExecutors_noDepartment_returnsEmpty() {
            project.setDepartment(null);
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

            List<UserResponse> result = taskService.getAssignableExecutors(PROJECT_ID);

            assertThat(result).isEmpty();
            verify(userRepository, never()).findByRoleAndDepartmentId(any(), any());
        }

        @Test
        @DisplayName("專案有 department 且存在 EXECUTOR 時回傳列表")
        void getAssignableExecutors_hasExecutors_returnsList() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(userRepository.findByRoleAndDepartmentId(Role.EXECUTOR, DEPT_ID))
                    .thenReturn(List.of(executorUser));

            List<UserResponse> result = taskService.getAssignableExecutors(PROJECT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(executorUser.getId());
            assertThat(result.get(0).name()).isEqualTo(executorUser.getName());
        }

        @Test
        @DisplayName("專案不存在時拋出 ResourceNotFoundException")
        void getAssignableExecutors_projectNotFound_throws() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getAssignableExecutors(PROJECT_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Project not found");
        }
    }
}
