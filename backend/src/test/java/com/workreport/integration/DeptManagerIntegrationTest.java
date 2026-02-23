package com.workreport.integration;

import com.workreport.enums.Role;
import com.workreport.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class DeptManagerIntegrationTest {

    private static final Long DEPT_MANAGER_USER_ID = 3L;

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private RestClient client;

    @BeforeEach
    void setUp() {
        String managerToken = jwtTokenProvider.generateToken(
                DEPT_MANAGER_USER_ID, "manager@company.com", Set.of(Role.DEPT_MANAGER));
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/dept")
                .defaultHeader("Authorization", "Bearer " + managerToken)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Test
    void getDeptOverview_returns200() {
        var response = client.get()
                .uri("/overview")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("deptName")).isEqualTo("研發部");
        assertThat(response.getBody().get("memberCount")).isNotNull();
        assertThat(response.getBody().get("members")).isNotNull();
    }

    @Test
    void getDeptOverview_containsMemberWorkEntries() {
        var response = client.get()
                .uri("/overview")
                .retrieve()
                .toEntity(Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("totalHoursThisMonth")).isNotNull();
    }
}
