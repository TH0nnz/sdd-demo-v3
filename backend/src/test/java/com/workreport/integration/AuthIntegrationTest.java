package com.workreport.integration;

import com.workreport.dto.auth.ChangePasswordRequest;
import com.workreport.dto.auth.LoginRequest;
import com.workreport.dto.auth.LoginResponse;
import com.workreport.entity.User;
import com.workreport.enums.Role;
import com.workreport.repository.UserRepository;
import com.workreport.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class AuthIntegrationTest {

    private static final String SEED_PASSWORD = "Welcome123";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (req, resp) -> {})
                .build();

        jdbcTemplate.execute(
                "UPDATE users SET failed_login_count = 0, locked_until = NULL, "
                        + "last_failed_login = NULL, active = true WHERE id <= 5");
    }

    private <T> ResponseEntity<T> postPublic(String uri, Object body, Class<T> type) {
        return restClient.post().uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(type);
    }

    private <T> ResponseEntity<T> postAuth(String token, String uri, Object body, Class<T> type) {
        return restClient.post().uri(uri)
                .headers(h -> { h.setBearerAuth(token); h.setContentType(MediaType.APPLICATION_JSON); })
                .body(body)
                .retrieve()
                .toEntity(type);
    }

    @Test
    void login_success_returnsTokenAndUserInfo() {
        var request = new LoginRequest("admin@company.com", SEED_PASSWORD);
        ResponseEntity<LoginResponse> response = postPublic("/api/auth/login", request, LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(response.getBody().user().email()).isEqualTo("admin@company.com");
        assertThat(response.getBody().user().roles()).contains(Role.ADMIN);
    }

    @Test
    void login_wrongPassword_returns401() {
        var request = new LoginRequest("admin@company.com", "WrongPassword");
        ResponseEntity<Map> response = postPublic("/api/auth/login", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_accountLockedAfter15Attempts_returns423() {
        jdbcTemplate.update(
                "UPDATE users SET failed_login_count = 15, locked_until = ?, last_failed_login = ? WHERE email = ?",
                LocalDateTime.now().plusMinutes(15), LocalDateTime.now(), "executor@company.com");

        var request = new LoginRequest("executor@company.com", SEED_PASSWORD);
        ResponseEntity<Map> response = postPublic("/api/auth/login", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
    }

    @Test
    void login_accountDeactivated_returns403() {
        jdbcTemplate.update("UPDATE users SET active = false WHERE email = ?", "hr@company.com");

        var request = new LoginRequest("hr@company.com", SEED_PASSWORD);
        ResponseEntity<Map> response = postPublic("/api/auth/login", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void login_firstLogin_forcePasswordChangeTrue() {
        var request = new LoginRequest("pm@company.com", SEED_PASSWORD);
        ResponseEntity<LoginResponse> response = postPublic("/api/auth/login", request, LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().forcePasswordChange()).isTrue();
    }

    @Test
    void changePassword_validComplexity_success() {
        User user = userRepository.findByEmail("manager@company.com").orElseThrow();
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRoles());
        var request = new ChangePasswordRequest(SEED_PASSWORD, "NewPass123");

        ResponseEntity<Map> response = postAuth(token, "/api/auth/change-password", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("message", "Password changed successfully");
    }

    @Test
    void changePassword_invalidComplexity_returns400() {
        User user = userRepository.findByEmail("executor@company.com").orElseThrow();
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRoles());
        var request = new ChangePasswordRequest(SEED_PASSWORD, "weak");

        ResponseEntity<Map> response = postAuth(token, "/api/auth/change-password", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void changePassword_wrongCurrentPassword_returns401() {
        User user = userRepository.findByEmail("hr@company.com").orElseThrow();
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRoles());
        var request = new ChangePasswordRequest("WrongCurrent1", "NewPass123");

        ResponseEntity<Map> response = postAuth(token, "/api/auth/change-password", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
