package com.workreport.integration;

import com.workreport.dto.user.CreateUserRequest;
import com.workreport.dto.user.UserResponse;
import com.workreport.enums.Role;
import com.workreport.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class HrIntegrationTest {

    private static final Long HR_USER_ID = 5L;

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RestClient client;

    @BeforeEach
    void setUp() {
        String hrToken = jwtTokenProvider.generateToken(HR_USER_ID, "hr@company.com", Set.of(Role.HR));
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/users")
                .defaultHeader("Authorization", "Bearer " + hrToken)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        // Clean up non-seed users
        jdbcTemplate.execute("DELETE FROM user_roles WHERE user_id > 5");
        jdbcTemplate.execute("DELETE FROM users WHERE id > 5");
    }

    @Test
    void createUser_returns201() {
        CreateUserRequest request = new CreateUserRequest(
                "HR 新增使用者", "hr-new@company.com", 1L, Set.of(Role.EXECUTOR));

        var response = client.post()
                .body(request)
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("temporaryPassword")).isNotNull();
        Map<String, Object> user = (Map<String, Object>) response.getBody().get("user");
        assertThat(user.get("name")).isEqualTo("HR 新增使用者");
        assertThat(user.get("email")).isEqualTo("hr-new@company.com");
    }

    @Test
    void listUsers_returns200() {
        var response = client.get()
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("content")).isNotNull();
    }

    @Test
    void disableAndEnableUser_flow() {
        // Disable seed user (executor id=4)
        var disableResp = client.post()
                .uri("/4/disable")
                .retrieve()
                .toEntity(UserResponse.class);

        assertThat(disableResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(disableResp.getBody()).isNotNull();
        assertThat(disableResp.getBody().active()).isFalse();

        // Enable user back
        var enableResp = client.post()
                .uri("/4/enable")
                .retrieve()
                .toEntity(UserResponse.class);

        assertThat(enableResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(enableResp.getBody()).isNotNull();
        assertThat(enableResp.getBody().active()).isTrue();
    }
}
