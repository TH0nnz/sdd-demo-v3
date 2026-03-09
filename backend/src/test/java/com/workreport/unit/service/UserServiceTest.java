package com.workreport.unit.service;

import com.workreport.dto.common.PageResponse;
import com.workreport.dto.user.CreateUserRequest;
import com.workreport.dto.user.CreateUserResponse;
import com.workreport.dto.user.ResetPasswordResponse;
import com.workreport.dto.user.UpdateUserRequest;
import com.workreport.dto.user.UserResponse;
import com.workreport.entity.Department;
import com.workreport.entity.Project;
import com.workreport.entity.Task;
import com.workreport.entity.User;
import com.workreport.enums.AuditActionType;
import com.workreport.enums.NotificationType;
import com.workreport.enums.Role;
import com.workreport.enums.TaskStatus;
import com.workreport.exception.BusinessRuleException;
import com.workreport.repository.DepartmentRepository;
import com.workreport.repository.TaskRepository;
import com.workreport.repository.UserRepository;
import com.workreport.service.AuditLogService;
import com.workreport.service.NotificationService;
import com.workreport.service.UserService;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
class UserServiceTest {

    private static final Long TEST_ACTOR_ID = 999L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    private UserService userService;

    private Department department;
    private User user;
    private User pmUser;

    @BeforeEach
    void setUp() {
        Authentication auth = new UsernamePasswordAuthenticationToken(TEST_ACTOR_ID, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        userService = new UserService(
                userRepository,
                departmentRepository,
                taskRepository,
                notificationService,
                auditLogService,
                passwordEncoder
        );

        department = new Department();
        department.setId(1L);
        department.setName("IT");

        pmUser = new User();
        pmUser.setId(2L);
        pmUser.setName("PM User");
        pmUser.setEmail("pm@example.com");
        pmUser.setDepartment(department);
        pmUser.setActive(true);
        pmUser.setRoles(Set.of(Role.PM));

        user = new User();
        user.setId(100L);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPasswordHash("$2a$10$hashed");
        user.setDepartment(department);
        user.setActive(true);
        user.setRoles(Set.of(Role.EXECUTOR));
        user.setLockedUntil(null);
        user.setFailedLoginCount(0);
        user.setCreatedAt(LocalDateTime.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("email 已存在時拋出 BusinessRuleException")
        void createUser_emailExists_throwsBusinessRuleException() {
            CreateUserRequest request = new CreateUserRequest(
                    "New User",
                    "existing@example.com",
                    1L,
                    Set.of(Role.EXECUTOR)
            );
            when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Email already exists")
                    .hasMessageContaining("existing@example.com");

            verify(userRepository, never()).save(any());
            verify(departmentRepository, never()).findById(any());
        }

        @Test
        @DisplayName("department 不存在時拋出 BusinessRuleException NOT_FOUND")
        void createUser_departmentNotFound_throwsNotFound() {
            CreateUserRequest request = new CreateUserRequest(
                    "New User",
                    "new@example.com",
                    999L,
                    Set.of(Role.EXECUTOR)
            );
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(ex -> {
                        BusinessRuleException e = (BusinessRuleException) ex;
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(e.getMessage()).contains("Department not found");
                    });

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("成功時產生 tempPassword、建立 User、save、回傳 CreateUserResponse")
        void createUser_success_createsUserAndReturnsResponse() {
            CreateUserRequest request = new CreateUserRequest(
                    "New User",
                    "new@example.com",
                    1L,
                    Set.of(Role.EXECUTOR, Role.DEPT_MANAGER)
            );
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
            when(passwordEncoder.encode(any(String.class))).thenReturn("$2a$10$encoded");
            User savedUser = new User();
            savedUser.setId(200L);
            savedUser.setName("New User");
            savedUser.setEmail("new@example.com");
            savedUser.setDepartment(department);
            savedUser.setActive(true);
            savedUser.setRoles(Set.of(Role.EXECUTOR, Role.DEPT_MANAGER));
            savedUser.setCreatedAt(LocalDateTime.now());
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            CreateUserResponse response = userService.createUser(request);

            assertThat(response.user()).isNotNull();
            assertThat(response.user().name()).isEqualTo("New User");
            assertThat(response.user().email()).isEqualTo("new@example.com");
            assertThat(response.user().departmentId()).isEqualTo(1L);
            assertThat(response.user().roles()).containsExactlyInAnyOrder(Role.EXECUTOR, Role.DEPT_MANAGER);
            assertThat(response.temporaryPassword()).hasSize(12);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User captured = userCaptor.getValue();
            assertThat(captured.getName()).isEqualTo("New User");
            assertThat(captured.getEmail()).isEqualTo("new@example.com");
            assertThat(captured.getDepartment()).isEqualTo(department);
            assertThat(captured.isActive()).isTrue();
            assertThat(captured.isPasswordChanged()).isFalse();
            assertThat(captured.getPasswordHash()).isEqualTo("$2a$10$encoded");
            verify(passwordEncoder).encode(any(String.class));
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        @DisplayName("角色變更時呼叫 auditLogService.log(ROLE_CHANGE)")
        void updateUser_roleChange_callsAuditLog() {
            UpdateUserRequest request = new UpdateUserRequest(
                    "Updated Name",
                    1L,
                    Set.of(Role.PM, Role.ADMIN)
            );
            when(userRepository.findById(100L)).thenReturn(Optional.of(user));
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
            when(taskRepository.findByAssigneeIdAndStatusIn(100L, List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS)))
                    .thenReturn(List.of());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            userService.updateUser(100L, request);

            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(auditLogService).log(
                    eq(AuditActionType.ROLE_CHANGE),
                    eq(TEST_ACTOR_ID),
                    eq("User"),
                    eq(100L),
                    messageCaptor.capture()
            );
            assertThat(messageCaptor.getValue()).contains("角色已變更").contains("EXECUTOR");
        }

        @Test
        @DisplayName("角色未變更時不呼叫 auditLogService.log(ROLE_CHANGE)")
        void updateUser_noRoleChange_doesNotCallAuditLogForRoleChange() {
            UpdateUserRequest request = new UpdateUserRequest(
                    "Updated Name",
                    1L,
                    Set.of(Role.EXECUTOR)
            );
            when(userRepository.findById(100L)).thenReturn(Optional.of(user));
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserResponse response = userService.updateUser(100L, request);

            assertThat(response.name()).isEqualTo("Updated Name");
            verify(auditLogService, never()).log(
                    eq(AuditActionType.ROLE_CHANGE),
                    any(),
                    any(),
                    any(),
                    any()
            );
        }

        @Test
        @DisplayName("成功時更新 name、department、roles 並回傳 UserResponse")
        void updateUser_success_returnsUserResponse() {
            Department newDept = new Department();
            newDept.setId(2L);
            newDept.setName("HR");
            UpdateUserRequest request = new UpdateUserRequest(
                    "Updated Name",
                    2L,
                    Set.of(Role.EXECUTOR)
            );
            when(userRepository.findById(100L)).thenReturn(Optional.of(user));
            when(departmentRepository.findById(2L)).thenReturn(Optional.of(newDept));
            when(taskRepository.findByAssigneeIdAndStatusIn(100L, List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS)))
                    .thenReturn(List.of());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserResponse response = userService.updateUser(100L, request);

            assertThat(response.name()).isEqualTo("Updated Name");
            assertThat(response.departmentId()).isEqualTo(2L);
            assertThat(response.departmentName()).isEqualTo("HR");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User captured = userCaptor.getValue();
            assertThat(captured.getName()).isEqualTo("Updated Name");
            assertThat(captured.getDepartment()).isEqualTo(newDept);
        }

        @Test
        @DisplayName("user 不存在時拋出 NOT_FOUND")
        void updateUser_userNotFound_throwsNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(999L,
                    new UpdateUserRequest("Name", 1L, Set.of(Role.EXECUTOR))))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(ex -> assertThat(((BusinessRuleException) ex).getStatus())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("department 不存在時拋出 NOT_FOUND")
        void updateUser_departmentNotFound_throwsNotFound() {
            when(userRepository.findById(100L)).thenReturn(Optional.of(user));
            when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(100L,
                    new UpdateUserRequest("Name", 999L, Set.of(Role.EXECUTOR))))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(ex -> assertThat(((BusinessRuleException) ex).getStatus())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("有未完成 Task 時變更部門或角色拋出 CONFLICT")
        void updateUser_hasUncompletedTasks_roleOrDeptChange_throwsConflict() {
            Task uncompletedTask = new Task();
            uncompletedTask.setId(1L);
            uncompletedTask.setStatus(TaskStatus.IN_PROGRESS);
            UpdateUserRequest request = new UpdateUserRequest(
                    "Updated Name",
                    2L,
                    Set.of(Role.PM)
            );
            Department newDept = new Department();
            newDept.setId(2L);
            newDept.setName("HR");
            when(userRepository.findById(100L)).thenReturn(Optional.of(user));
            when(departmentRepository.findById(2L)).thenReturn(Optional.of(newDept));
            when(taskRepository.findByAssigneeIdAndStatusIn(100L, List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS)))
                    .thenReturn(List.of(uncompletedTask));

            assertThatThrownBy(() -> userService.updateUser(100L, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(ex -> {
                        BusinessRuleException bre = (BusinessRuleException) ex;
                        assertThat(bre.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(bre.getMessage()).isEqualTo("該人員目前還有尚未完成的Task，無法變更部門或角色");
                    });
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("disableUser")
    class DisableUser {

        @Test
        @DisplayName("停用自己時拋出 FORBIDDEN")
        void disableUser_self_throwsForbidden() {
            assertThatThrownBy(() -> userService.disableUser(TEST_ACTOR_ID))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(ex -> {
                        BusinessRuleException bre = (BusinessRuleException) ex;
                        assertThat(bre.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                        assertThat(bre.getMessage()).isEqualTo("無法停用自己");
                    });
            verify(userRepository, never()).findById(any());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("成功時 setActive(false)、auditLog ACCOUNT_DEACTIVATE")
        void disableUser_success_setsActiveFalseAndLogs() {
            when(userRepository.findById(100L)).thenReturn(Optional.of(user));
            when(taskRepository.findByAssigneeIdAndStatusIn(100L, List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS)))
                    .thenReturn(List.of());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserResponse response = userService.disableUser(100L);

            assertThat(response.active()).isFalse();

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().isActive()).isFalse();

            verify(auditLogService).log(
                    eq(AuditActionType.ACCOUNT_DEACTIVATE),
                    eq(TEST_ACTOR_ID),
                    eq("User"),
                    eq(100L),
                    org.mockito.ArgumentMatchers.contains("帳號已停用")
            );
            verify(notificationService, never()).notify(any(), any(), any(), any());
        }

        @Test
        @DisplayName("有 PENDING/IN_PROGRESS task 時清空 assignee 並 notify PM")
        void disableUser_withTasks_clearsAssigneeAndNotifiesPm() {
            Project project = new Project();
            project.setId(10L);
            project.setPm(pmUser);
            Task task = new Task();
            task.setId(1L);
            task.setName("Task A");
            task.setProject(project);
            task.setAssignee(user);
            task.setStatus(TaskStatus.PENDING);
            task.setBudgetHours(BigDecimal.TEN);
            task.setConsumedHours(BigDecimal.ZERO);

            when(userRepository.findById(100L)).thenReturn(Optional.of(user));
            when(taskRepository.findByAssigneeIdAndStatusIn(100L, List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS)))
                    .thenReturn(List.of(task));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            userService.disableUser(100L);

            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository).save(taskCaptor.capture());
            assertThat(taskCaptor.getValue().getAssignee()).isNull();

            ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
            verify(notificationService).notify(
                    eq(2L),
                    eq(NotificationType.TASK_UNASSIGNED),
                    eq("Task 已轉為未指派"),
                    contentCaptor.capture()
            );
            assertThat(contentCaptor.getValue()).contains("Test User").contains("Task A");
        }
    }

    @Nested
    @DisplayName("enableUser")
    class EnableUser {

        @Test
        @DisplayName("成功時 setActive(true)、清 lockedUntil/failedLoginCount、auditLog ACCOUNT_ACTIVATE")
        void enableUser_success_setsActiveAndClearsLock() {
            user.setActive(false);
            user.setLockedUntil(LocalDateTime.now().plusHours(1));
            user.setFailedLoginCount(5);
            when(userRepository.findById(100L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserResponse response = userService.enableUser(100L);

            assertThat(response.active()).isTrue();

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User captured = userCaptor.getValue();
            assertThat(captured.isActive()).isTrue();
            assertThat(captured.getLockedUntil()).isNull();
            assertThat(captured.getFailedLoginCount()).isZero();

            verify(auditLogService).log(
                    eq(AuditActionType.ACCOUNT_ACTIVATE),
                    eq(TEST_ACTOR_ID),
                    eq("User"),
                    eq(100L),
                    org.mockito.ArgumentMatchers.contains("帳號已啟用")
            );
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("成功時產生新密碼、更新 user、回傳 ResetPasswordResponse")
        void resetPassword_success_returnsNewPassword() {
            when(userRepository.findById(100L)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(any(String.class))).thenReturn("$2a$10$newEncoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            ResetPasswordResponse response = userService.resetPassword(100L);

            assertThat(response.temporaryPassword()).hasSize(12);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User captured = userCaptor.getValue();
            assertThat(captured.getPasswordHash()).isEqualTo("$2a$10$newEncoded");
            assertThat(captured.isPasswordChanged()).isFalse();
        }
    }

    @Nested
    @DisplayName("getUser")
    class GetUser {

        @Test
        @DisplayName("user 存在時回傳 UserResponse")
        void getUser_exists_returnsUserResponse() {
            when(userRepository.findById(100L)).thenReturn(Optional.of(user));

            UserResponse response = userService.getUser(100L);

            assertThat(response.id()).isEqualTo(100L);
            assertThat(response.name()).isEqualTo("Test User");
            assertThat(response.email()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("user 不存在時拋出 BusinessRuleException NOT_FOUND")
        void getUser_notFound_throwsNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUser(999L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("User not found")
                    .satisfies(ex -> assertThat(((BusinessRuleException) ex).getStatus())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getUsers")
    class GetUsers {

        @Test
        @DisplayName("回傳分頁 UserResponse")
        void getUsers_returnsPageResponse() {
            Pageable pageable = PageRequest.of(0, 10);
            when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user), pageable, 1));

            PageResponse<UserResponse> response = userService.getUsers(pageable);

            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).id()).isEqualTo(100L);
            assertThat(response.page()).isZero();
            assertThat(response.size()).isEqualTo(10);
            assertThat(response.totalElements()).isOne();
            assertThat(response.totalPages()).isOne();
        }
    }

    @Nested
    @DisplayName("getUsersByRole")
    class GetUsersByRole {

        @Test
        @DisplayName("依 role 回傳 UserResponse 列表")
        void getUsersByRole_returnsList() {
            when(userRepository.findByRole(Role.PM)).thenReturn(List.of(pmUser));

            List<UserResponse> result = userService.getUsersByRole(Role.PM);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(2L);
            assertThat(result.get(0).name()).isEqualTo("PM User");
            assertThat(result.get(0).roles()).contains(Role.PM);
        }
    }
}
