package com.workreport.service;

import com.workreport.dto.common.PageResponse;
import com.workreport.dto.user.CreateUserRequest;
import com.workreport.dto.user.CreateUserResponse;
import com.workreport.dto.user.ResetPasswordResponse;
import com.workreport.dto.user.UpdateUserRequest;
import com.workreport.dto.user.UserResponse;
import com.workreport.entity.Task;
import com.workreport.entity.Department;
import com.workreport.entity.User;
import com.workreport.enums.AuditActionType;
import com.workreport.enums.NotificationType;
import com.workreport.enums.Role;
import com.workreport.enums.TaskStatus;
import com.workreport.exception.BusinessRuleException;
import com.workreport.repository.DepartmentRepository;
import com.workreport.repository.TaskRepository;
import com.workreport.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class UserService {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TEMP_PASSWORD_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TaskRepository taskRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       DepartmentRepository departmentRepository,
                       TaskRepository taskRepository,
                       NotificationService notificationService,
                       AuditLogService auditLogService,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
    }

    public CreateUserResponse createUser(CreateUserRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessRuleException("Email already exists: " + request.email());
        }

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new BusinessRuleException("Department not found",
                        HttpStatus.NOT_FOUND));

        String tempPassword = generateTempPassword();

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setPasswordChanged(false);
        user.setDepartment(department);
        user.setActive(true);
        user.setRoles(request.roles());

        User saved = userRepository.save(user);
        return new CreateUserResponse(UserResponse.from(saved), tempPassword);
    }

    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = findUserById(userId);
        Set<Role> originalRoles = new HashSet<>(user.getRoles());

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new BusinessRuleException("Department not found",
                        HttpStatus.NOT_FOUND));

        boolean roleChanged = !originalRoles.equals(request.roles());
        boolean departmentChanged = !user.getDepartment().getId().equals(request.departmentId());
        if (roleChanged || departmentChanged) {
            List<TaskStatus> nonTerminal = List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS);
            List<Task> uncompletedTasks = taskRepository.findByAssigneeIdAndStatusIn(userId, nonTerminal);
            if (!uncompletedTasks.isEmpty()) {
                throw new BusinessRuleException(
                        "該人員目前還有尚未完成的Task，無法變更部門或角色",
                        HttpStatus.CONFLICT
                );
            }
        }

        user.setName(request.name());
        user.setDepartment(department);
        user.setRoles(request.roles());

        if (!originalRoles.equals(request.roles())) {
            auditLogService.log(
                    AuditActionType.ROLE_CHANGE,
                    getCurrentUserId(),
                    "User",
                    userId,
                    "角色已變更: " + originalRoles + " -> " + request.roles()
            );
        }

        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse disableUser(Long userId) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId.equals(userId)) {
            throw new BusinessRuleException("無法停用自己", HttpStatus.FORBIDDEN);
        }
        User user = findUserById(userId);
        user.setActive(false);

        List<TaskStatus> nonTerminal = List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS);
        List<Task> affectedTasks = taskRepository.findByAssigneeIdAndStatusIn(userId, nonTerminal);
        for (Task task : affectedTasks) {
            task.setAssignee(null);
            taskRepository.save(task);
            notificationService.notify(
                    task.getProject().getPm().getId(),
                    NotificationType.TASK_UNASSIGNED,
                    "Task 已轉為未指派",
                    "使用者「" + user.getName() + "」已停用，Task「" + task.getName() + "」已轉為未指派"
            );
        }

        auditLogService.log(
                AuditActionType.ACCOUNT_DEACTIVATE,
                getCurrentUserId(),
                "User",
                userId,
                "帳號已停用: " + user.getEmail()
        );

        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse enableUser(Long userId) {
        User user = findUserById(userId);
        user.setActive(true);
        user.setLockedUntil(null);
        user.setFailedLoginCount(0);

        auditLogService.log(
                AuditActionType.ACCOUNT_ACTIVATE,
                getCurrentUserId(),
                "User",
                userId,
                "帳號已啟用: " + user.getEmail()
        );

        return UserResponse.from(userRepository.save(user));
    }

    public ResetPasswordResponse resetPassword(Long userId) {
        User user = findUserById(userId);
        String tempPassword = generateTempPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setPasswordChanged(false);
        userRepository.save(user);
        return new ResetPasswordResponse(tempPassword);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(Pageable pageable) {
        Page<UserResponse> page = userRepository.findAll(pageable)
                .map(UserResponse::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        return UserResponse.from(findUserById(userId));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRole(Role role) {
        return userRepository.findByRole(role).stream()
                .map(UserResponse::from)
                .toList();
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("User not found",
                        HttpStatus.NOT_FOUND));
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
