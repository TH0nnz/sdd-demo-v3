package com.workreport.unit.service;

import com.workreport.dto.user.CreateUserRequest;
import com.workreport.dto.user.CreateUserResponse;
import com.workreport.dto.user.ResetPasswordResponse;
import com.workreport.dto.user.UpdateUserRequest;
import com.workreport.dto.user.UserResponse;
import com.workreport.entity.Department;
import com.workreport.entity.User;
import com.workreport.enums.Role;
import com.workreport.exception.BusinessRuleException;
import com.workreport.repository.DepartmentRepository;
import com.workreport.repository.UserRepository;
import com.workreport.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private Department department;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(1L);
        department.setName("研發部");
    }

    @Test
    void createUser_success() {
        CreateUserRequest request = new CreateUserRequest(
                "新使用者", "newuser@company.com", 1L, Set.of(Role.EXECUTOR));

        when(userRepository.findByEmail("newuser@company.com")).thenReturn(Optional.empty());
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            u.setCreatedAt(LocalDateTime.now());
            return u;
        });

        CreateUserResponse response = userService.createUser(request);

        assertThat(response.user().name()).isEqualTo("新使用者");
        assertThat(response.user().email()).isEqualTo("newuser@company.com");
        assertThat(response.user().departmentName()).isEqualTo("研發部");
        assertThat(response.user().roles()).containsExactly(Role.EXECUTOR);
        assertThat(response.temporaryPassword()).isNotBlank();
        assertThat(response.temporaryPassword()).hasSize(12);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_duplicateEmail_reject() {
        CreateUserRequest request = new CreateUserRequest(
                "重複使用者", "existing@company.com", 1L, Set.of(Role.EXECUTOR));

        User existing = new User();
        existing.setEmail("existing@company.com");
        when(userRepository.findByEmail("existing@company.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void updateUser_roles() {
        User user = createTestUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest request = new UpdateUserRequest("更新名稱", 1L, Set.of(Role.PM, Role.EXECUTOR));
        UserResponse response = userService.updateUser(1L, request);

        assertThat(response.name()).isEqualTo("更新名稱");
        assertThat(response.roles()).containsExactlyInAnyOrder(Role.PM, Role.EXECUTOR);
    }

    @Test
    void disableUser_success() {
        User user = createTestUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.disableUser(1L);

        assertThat(response.active()).isFalse();
    }

    @Test
    void enableUser_success() {
        User user = createTestUser(1L);
        user.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.enableUser(1L);

        assertThat(response.active()).isTrue();
    }

    @Test
    void resetPassword_generatesTempPassword() {
        User user = createTestUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ResetPasswordResponse response = userService.resetPassword(1L);

        assertThat(response.temporaryPassword()).isNotBlank();
        assertThat(response.temporaryPassword()).hasSize(12);
        verify(passwordEncoder).encode(anyString());
        verify(userRepository).save(any(User.class));
    }

    private User createTestUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Test User");
        user.setEmail("test@company.com");
        user.setPasswordHash("hashed");
        user.setPasswordChanged(false);
        user.setDepartment(department);
        user.setActive(true);
        user.setRoles(Set.of(Role.EXECUTOR));
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
