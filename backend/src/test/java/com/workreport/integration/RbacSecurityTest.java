package com.workreport.integration;

import com.workreport.enums.Role;
import com.workreport.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RBAC (Role-Based Access Control) integration test.
 * Verifies that each role is denied access to endpoints belonging to other roles.
 *
 * Role → Endpoint mapping:
 *   EXECUTOR:     /api/work-entries, /api/my-tasks
 *   PM:           /api/pm/projects, /api/hours-requests (shared with ADMIN)
 *   ADMIN:        /api/projects, /api/admin/hours-requests, /api/hours-requests (shared with PM)
 *   HR:           /api/users
 *   DEPT_MANAGER: /api/dept/overview
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class RbacSecurityTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private RestClient restClient;

    // Tokens per role (user IDs from V2__seed_data.sql)
    private String executorToken;
    private String pmToken;
    private String adminToken;
    private String hrToken;
    private String deptManagerToken;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        executorToken = jwtTokenProvider.generateToken(4L, "executor@company.com", Set.of(Role.EXECUTOR));
        pmToken = jwtTokenProvider.generateToken(2L, "pm@company.com", Set.of(Role.PM));
        adminToken = jwtTokenProvider.generateToken(1L, "admin@company.com", Set.of(Role.ADMIN));
        hrToken = jwtTokenProvider.generateToken(5L, "hr@company.com", Set.of(Role.HR));
        deptManagerToken = jwtTokenProvider.generateToken(3L, "manager@company.com", Set.of(Role.DEPT_MANAGER));
    }

    private int get(String token, String uri) {
        try {
            return restClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode().value();
        } catch (RestClientResponseException e) {
            return e.getStatusCode().value();
        }
    }

    // ─── EXECUTOR trying other roles' endpoints ───

    @Test
    void executorCannotAccessAdminEndpoints() {
        assertThat(get(executorToken, "/api/projects")).isEqualTo(403);
        assertThat(get(executorToken, "/api/admin/hours-requests")).isEqualTo(403);
    }

    @Test
    void executorCannotAccessPmEndpoints() {
        assertThat(get(executorToken, "/api/pm/projects")).isEqualTo(403);
        assertThat(get(executorToken, "/api/hours-requests")).isEqualTo(403);
    }

    @Test
    void executorCannotAccessHrEndpoints() {
        assertThat(get(executorToken, "/api/users")).isEqualTo(403);
    }

    @Test
    void executorCannotAccessDeptManagerEndpoints() {
        assertThat(get(executorToken, "/api/dept/overview")).isEqualTo(403);
    }

    // ─── PM trying other roles' endpoints ───

    @Test
    void pmCannotAccessExecutorEndpoints() {
        assertThat(get(pmToken, "/api/work-entries")).isEqualTo(403);
        assertThat(get(pmToken, "/api/my-tasks")).isEqualTo(403);
    }

    @Test
    void pmCannotAccessAdminOnlyEndpoints() {
        assertThat(get(pmToken, "/api/projects")).isEqualTo(403);
        assertThat(get(pmToken, "/api/admin/hours-requests")).isEqualTo(403);
    }

    @Test
    void pmCannotAccessHrEndpoints() {
        assertThat(get(pmToken, "/api/users")).isEqualTo(403);
    }

    @Test
    void pmCannotAccessDeptManagerEndpoints() {
        assertThat(get(pmToken, "/api/dept/overview")).isEqualTo(403);
    }

    // ─── ADMIN trying other roles' endpoints ───

    @Test
    void adminCannotAccessExecutorEndpoints() {
        assertThat(get(adminToken, "/api/work-entries")).isEqualTo(403);
        assertThat(get(adminToken, "/api/my-tasks")).isEqualTo(403);
    }

    @Test
    void adminCannotAccessPmOnlyEndpoints() {
        assertThat(get(adminToken, "/api/pm/projects")).isEqualTo(403);
    }

    @Test
    void adminCannotAccessHrEndpoints() {
        assertThat(get(adminToken, "/api/users")).isEqualTo(403);
    }

    @Test
    void adminCannotAccessDeptManagerEndpoints() {
        assertThat(get(adminToken, "/api/dept/overview")).isEqualTo(403);
    }

    // ─── HR trying other roles' endpoints ───

    @Test
    void hrCannotAccessExecutorEndpoints() {
        assertThat(get(hrToken, "/api/work-entries")).isEqualTo(403);
        assertThat(get(hrToken, "/api/my-tasks")).isEqualTo(403);
    }

    @Test
    void hrCannotAccessAdminEndpoints() {
        assertThat(get(hrToken, "/api/projects")).isEqualTo(403);
        assertThat(get(hrToken, "/api/admin/hours-requests")).isEqualTo(403);
    }

    @Test
    void hrCannotAccessPmEndpoints() {
        assertThat(get(hrToken, "/api/pm/projects")).isEqualTo(403);
        assertThat(get(hrToken, "/api/hours-requests")).isEqualTo(403);
    }

    @Test
    void hrCannotAccessDeptManagerEndpoints() {
        assertThat(get(hrToken, "/api/dept/overview")).isEqualTo(403);
    }

    // ─── DEPT_MANAGER trying other roles' endpoints ───

    @Test
    void deptManagerCannotAccessExecutorEndpoints() {
        assertThat(get(deptManagerToken, "/api/work-entries")).isEqualTo(403);
        assertThat(get(deptManagerToken, "/api/my-tasks")).isEqualTo(403);
    }

    @Test
    void deptManagerCannotAccessAdminEndpoints() {
        assertThat(get(deptManagerToken, "/api/projects")).isEqualTo(403);
        assertThat(get(deptManagerToken, "/api/admin/hours-requests")).isEqualTo(403);
    }

    @Test
    void deptManagerCannotAccessPmEndpoints() {
        assertThat(get(deptManagerToken, "/api/pm/projects")).isEqualTo(403);
        assertThat(get(deptManagerToken, "/api/hours-requests")).isEqualTo(403);
    }

    @Test
    void deptManagerCannotAccessHrEndpoints() {
        assertThat(get(deptManagerToken, "/api/users")).isEqualTo(403);
    }
}
