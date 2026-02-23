package com.workreport.service;

import com.workreport.dto.auth.ChangePasswordRequest;
import com.workreport.dto.auth.LoginRequest;
import com.workreport.dto.auth.LoginResponse;
import com.workreport.entity.User;
import com.workreport.repository.UserRepository;
import com.workreport.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");
    private static final int MAX_FAILED_ATTEMPTS = 15;
    private static final int LOCK_DURATION_MINUTES = 15;
    private static final int FAILED_WINDOW_MINUTES = 5;

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is deactivated");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "Account is locked");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // Success: reset failed login state
        user.setFailedLoginCount(0);
        user.setLastFailedLogin(null);
        user.setLockedUntil(null);
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRoles());

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRoles(),
                user.getDepartment().getId(),
                user.getDepartment().getName()
        );

        return new LoginResponse(token, userInfo, !user.isPasswordChanged());
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }

        if (!PASSWORD_PATTERN.matcher(request.newPassword()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least 8 characters with uppercase, lowercase, and digit");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChanged(true);
        userRepository.save(user);
    }

    private void handleFailedLogin(User user) {
        LocalDateTime now = LocalDateTime.now();

        // Reset count if last failure was outside the window
        if (user.getLastFailedLogin() != null
                && user.getLastFailedLogin().isBefore(now.minusMinutes(FAILED_WINDOW_MINUTES))) {
            user.setFailedLoginCount(0);
        }

        user.setFailedLoginCount(user.getFailedLoginCount() + 1);
        user.setLastFailedLogin(now);

        if (user.getFailedLoginCount() >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(now.plusMinutes(LOCK_DURATION_MINUTES));
        }

        userRepository.save(user);
    }
}
