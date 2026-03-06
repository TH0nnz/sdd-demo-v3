package com.workreport.unit.service;

import com.workreport.dto.auth.ChangePasswordRequest;
import com.workreport.dto.auth.LoginRequest;
import com.workreport.dto.auth.LoginResponse;
import com.workreport.entity.Department;
import com.workreport.entity.User;
import com.workreport.enums.Role;
import com.workreport.repository.UserRepository;
import com.workreport.security.JwtTokenProvider;
import com.workreport.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
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
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    private User activeUser;
    private Department department;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider);
        department = new Department();
        department.setId(1L);
        department.setName("IT");

        activeUser = new User();
        activeUser.setId(100L);
        activeUser.setName("Test User");
        activeUser.setEmail("test@example.com");
        activeUser.setPasswordHash("$2a$10$hashed");
        activeUser.setPasswordChanged(true);
        activeUser.setDepartment(department);
        activeUser.setActive(true);
        activeUser.setFailedLoginCount(0);
        activeUser.setLastFailedLogin(null);
        activeUser.setLockedUntil(null);
        activeUser.setRoles(Set.of(Role.ADMIN, Role.PM));
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("成功時回傳 LoginResponse 並重置失敗計數與鎖定")
        void loginSuccess_returnsLoginResponseAndResetsFailedState() {
            LoginRequest request = new LoginRequest("test@example.com", "correct");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("correct", activeUser.getPasswordHash())).thenReturn(true);
            when(jwtTokenProvider.generateToken(100L, "test@example.com", activeUser.getRoles()))
                    .thenReturn("jwt-token");

            LoginResponse response = authService.login(request);

            assertThat(response.token()).isEqualTo("jwt-token");
            assertThat(response.forcePasswordChange()).isFalse();
            assertThat(response.user().id()).isEqualTo(100L);
            assertThat(response.user().name()).isEqualTo("Test User");
            assertThat(response.user().email()).isEqualTo("test@example.com");
            assertThat(response.user().roles()).containsExactlyInAnyOrder(Role.ADMIN, Role.PM);
            assertThat(response.user().departmentId()).isEqualTo(1L);
            assertThat(response.user().departmentName()).isEqualTo("IT");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.getFailedLoginCount()).isZero();
            assertThat(saved.getLastFailedLogin()).isNull();
            assertThat(saved.getLockedUntil()).isNull();
        }

        @Test
        @DisplayName("成功時若未改過密碼則 forcePasswordChange 為 true")
        void loginSuccess_whenPasswordNotChanged_forcePasswordChangeTrue() {
            activeUser.setPasswordChanged(false);
            LoginRequest request = new LoginRequest("test@example.com", "correct");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("correct", activeUser.getPasswordHash())).thenReturn(true);
            when(jwtTokenProvider.generateToken(100L, "test@example.com", activeUser.getRoles()))
                    .thenReturn("token");

            LoginResponse response = authService.login(request);

            assertThat(response.forcePasswordChange()).isTrue();
        }

        @Test
        @DisplayName("使用者不存在時拋出 UNAUTHORIZED")
        void login_userNotFound_throwsUnauthorized() {
            when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "any")))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid credentials");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("密碼錯誤時拋出 UNAUTHORIZED 並呼叫 handleFailedLogin")
        void login_wrongPassword_throwsUnauthorizedAndIncrementsFailedCount() {
            LoginRequest request = new LoginRequest("test@example.com", "wrong");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("wrong", activeUser.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid credentials");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.getFailedLoginCount()).isEqualTo(1);
            assertThat(saved.getLastFailedLogin()).isNotNull();
            assertThat(saved.getLockedUntil()).isNull();
            verify(jwtTokenProvider, never()).generateToken(any(), any(), any());
        }

        @Test
        @DisplayName("密碼錯誤且上次失敗超過時間窗則重置計數再累加")
        void login_wrongPassword_afterWindowResetsCountThenIncrements() {
            activeUser.setFailedLoginCount(5);
            activeUser.setLastFailedLogin(LocalDateTime.now().minusMinutes(10));
            LoginRequest request = new LoginRequest("test@example.com", "wrong");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("wrong", activeUser.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(ResponseStatusException.class);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getFailedLoginCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("失敗次數達上限時設定 lockedUntil")
        void login_wrongPassword_maxAttempts_setsLockedUntil() {
            activeUser.setFailedLoginCount(14);
            activeUser.setLastFailedLogin(LocalDateTime.now());
            LoginRequest request = new LoginRequest("test@example.com", "wrong");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("wrong", activeUser.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(ResponseStatusException.class);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.getFailedLoginCount()).isEqualTo(15);
            assertThat(saved.getLockedUntil()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("帳號停用時拋出 FORBIDDEN")
        void login_accountDeactivated_throwsForbidden() {
            activeUser.setActive(false);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> authService.login(new LoginRequest("test@example.com", "any")))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Account is deactivated");
            verify(passwordEncoder, never()).matches(any(), any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("帳號鎖定中時拋出 LOCKED")
        void login_accountLocked_throwsLocked() {
            activeUser.setLockedUntil(LocalDateTime.now().plusMinutes(10));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> authService.login(new LoginRequest("test@example.com", "correct")))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Account is locked");
            verify(passwordEncoder, never()).matches(any(), any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("鎖定已過期則可正常登入")
        void login_lockExpired_allowsLogin() {
            activeUser.setLockedUntil(LocalDateTime.now().minusMinutes(1));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("correct", activeUser.getPasswordHash())).thenReturn(true);
            when(jwtTokenProvider.generateToken(100L, "test@example.com", activeUser.getRoles()))
                    .thenReturn("token");

            LoginResponse response = authService.login(new LoginRequest("test@example.com", "correct"));

            assertThat(response.token()).isEqualTo("token");
            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("成功時更新 passwordHash 與 passwordChanged")
        void changePassword_success_updatesHashAndPasswordChanged() {
            ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "NewPass123");
            when(userRepository.findById(100L)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("oldPass", activeUser.getPasswordHash())).thenReturn(true);
            when(passwordEncoder.encode("NewPass123")).thenReturn("$2a$10$newHashed");

            authService.changePassword(100L, request);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.getPasswordHash()).isEqualTo("$2a$10$newHashed");
            assertThat(saved.isPasswordChanged()).isTrue();
        }

        @Test
        @DisplayName("使用者不存在時拋出 NOT_FOUND")
        void changePassword_userNotFound_throwsNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.changePassword(999L,
                    new ChangePasswordRequest("old", "NewPass123")))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("User not found");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("目前密碼錯誤時拋出 UNAUTHORIZED")
        void changePassword_wrongCurrentPassword_throwsUnauthorized() {
            ChangePasswordRequest request = new ChangePasswordRequest("wrongCurrent", "NewPass123");
            when(userRepository.findById(100L)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("wrongCurrent", activeUser.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> authService.changePassword(100L, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Current password is incorrect");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("新密碼不符規則（少於 8 字元）時拋出 BAD_REQUEST")
        void changePassword_newPasswordTooShort_throwsBadRequest() {
            ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "Short1");
            when(userRepository.findById(100L)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("oldPass", activeUser.getPasswordHash())).thenReturn(true);

            assertThatThrownBy(() -> authService.changePassword(100L, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Password must be at least 8 characters");
            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("新密碼無大寫時拋出 BAD_REQUEST")
        void changePassword_newPasswordNoUppercase_throwsBadRequest() {
            ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "lowercase123");
            when(userRepository.findById(100L)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("oldPass", activeUser.getPasswordHash())).thenReturn(true);

            assertThatThrownBy(() -> authService.changePassword(100L, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("uppercase");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("新密碼無小寫時拋出 BAD_REQUEST")
        void changePassword_newPasswordNoLowercase_throwsBadRequest() {
            ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "UPPERCASE123");
            when(userRepository.findById(100L)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("oldPass", activeUser.getPasswordHash())).thenReturn(true);

            assertThatThrownBy(() -> authService.changePassword(100L, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("lowercase");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("新密碼無數字時拋出 BAD_REQUEST")
        void changePassword_newPasswordNoDigit_throwsBadRequest() {
            ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "NoDigitHere");
            when(userRepository.findById(100L)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("oldPass", activeUser.getPasswordHash())).thenReturn(true);

            assertThatThrownBy(() -> authService.changePassword(100L, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("digit");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("新密碼符合規則時成功")
        void changePassword_validNewPassword_succeeds() {
            ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "ValidPass1");
            when(userRepository.findById(100L)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("oldPass", activeUser.getPasswordHash())).thenReturn(true);
            when(passwordEncoder.encode("ValidPass1")).thenReturn("encoded");

            authService.changePassword(100L, request);

            verify(userRepository).save(eq(activeUser));
            assertThat(activeUser.getPasswordHash()).isEqualTo("encoded");
            assertThat(activeUser.isPasswordChanged()).isTrue();
        }
    }
}
