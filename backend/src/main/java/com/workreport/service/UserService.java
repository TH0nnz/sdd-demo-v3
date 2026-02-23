package com.workreport.service;

import com.workreport.dto.common.PageResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
@Transactional
public class UserService {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TEMP_PASSWORD_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       DepartmentRepository departmentRepository,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
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

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new BusinessRuleException("Department not found",
                        HttpStatus.NOT_FOUND));

        user.setName(request.name());
        user.setDepartment(department);
        user.setRoles(request.roles());

        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse disableUser(Long userId) {
        User user = findUserById(userId);
        user.setActive(false);
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse enableUser(Long userId) {
        User user = findUserById(userId);
        user.setActive(true);
        user.setLockedUntil(null);
        user.setFailedLoginCount(0);
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

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
