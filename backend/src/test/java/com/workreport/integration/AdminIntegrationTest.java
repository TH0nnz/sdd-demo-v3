package com.workreport.integration;

import com.workreport.dto.hoursrequest.HoursRequestResponse;
import com.workreport.dto.hoursrequest.ReviewHoursRequestRequest;
import com.workreport.dto.project.CreateProjectRequest;
import com.workreport.dto.project.ProjectResponse;
import com.workreport.dto.project.UpdateProjectRequest;
import com.workreport.enums.HoursRequestStatus;
import com.workreport.enums.ProjectStatus;
import com.workreport.enums.Role;
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

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class AdminIntegrationTest {

    private static final Long ADMIN_USER_ID = 1L;
    private static final Long PM_USER_ID = 2L;
    private static final Long DEPT_ID = 1L;

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RestClient restClient;
    private String adminToken;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (req, resp) -> {})
                .build();
        adminToken = jwtTokenProvider.generateToken(ADMIN_USER_ID, "admin@company.com", Set.of(Role.ADMIN));

        jdbcTemplate.execute("DELETE FROM hours_request");
        jdbcTemplate.execute("DELETE FROM work_entry");
        jdbcTemplate.execute("DELETE FROM task WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM project WHERE id > 0");
    }

    private <T> ResponseEntity<T> doPost(String uri, Object body, Class<T> type) {
        return restClient.post().uri(uri)
                .headers(h -> { h.setBearerAuth(adminToken); h.setContentType(MediaType.APPLICATION_JSON); })
                .body(body)
                .retrieve()
                .toEntity(type);
    }

    private <T> ResponseEntity<T> doGet(String uri, Class<T> type) {
        return restClient.get().uri(uri)
                .headers(h -> h.setBearerAuth(adminToken))
                .retrieve()
                .toEntity(type);
    }

    private <T> ResponseEntity<T> doPut(String uri, Object body, Class<T> type) {
        return restClient.put().uri(uri)
                .headers(h -> { h.setBearerAuth(adminToken); h.setContentType(MediaType.APPLICATION_JSON); })
                .body(body)
                .retrieve()
                .toEntity(type);
    }

    private ResponseEntity<Void> doDelete(String uri) {
        return restClient.delete().uri(uri)
                .headers(h -> h.setBearerAuth(adminToken))
                .retrieve()
                .toBodilessEntity();
    }

    // --- Project CRUD ---

    @Test
    void createProject_returns201() {
        var request = new CreateProjectRequest("Admin Test Project", new BigDecimal("300.0"), PM_USER_ID, DEPT_ID);
        ResponseEntity<ProjectResponse> response = doPost("/api/projects", request, ProjectResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Admin Test Project");
        assertThat(response.getBody().totalBudgetHours()).isEqualByComparingTo(new BigDecimal("300.0"));
    }

    @Test
    void listProjects_returns200() {
        doPost("/api/projects",
                new CreateProjectRequest("List Project", new BigDecimal("100.0"), PM_USER_ID, DEPT_ID), ProjectResponse.class);

        ResponseEntity<Map> response = doGet("/api/projects", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("content")).isNotNull();
    }

    @Test
    void updateProject_returns200() {
        ResponseEntity<ProjectResponse> createResp = doPost("/api/projects",
                new CreateProjectRequest("Original Project", new BigDecimal("100.0"), PM_USER_ID, DEPT_ID), ProjectResponse.class);
        Long projectId = createResp.getBody().id();

        var updateReq = new UpdateProjectRequest("Updated Project", new BigDecimal("500.0"), PM_USER_ID, DEPT_ID);
        ResponseEntity<ProjectResponse> response = doPut("/api/projects/" + projectId, updateReq, ProjectResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Updated Project");
        assertThat(response.getBody().totalBudgetHours()).isEqualByComparingTo(new BigDecimal("500.0"));
        assertThat(response.getBody().departmentId()).isEqualTo(DEPT_ID);
    }

    @Test
    void closeProject_returns200() {
        ResponseEntity<ProjectResponse> createResp = doPost("/api/projects",
                new CreateProjectRequest("Project To Close", new BigDecimal("100.0"), PM_USER_ID, DEPT_ID), ProjectResponse.class);
        Long projectId = createResp.getBody().id();

        ResponseEntity<ProjectResponse> response = doPost("/api/projects/" + projectId + "/close", "", ProjectResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(ProjectStatus.CLOSED);
    }

    @Test
    void deleteProject_noTasks_returns204() {
        ResponseEntity<ProjectResponse> createResp = doPost("/api/projects",
                new CreateProjectRequest("Project To Delete", new BigDecimal("50.0"), PM_USER_ID, DEPT_ID), ProjectResponse.class);
        Long projectId = createResp.getBody().id();

        ResponseEntity<Void> response = doDelete("/api/projects/" + projectId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // --- Hours Request Review ---

    @Test
    void approveHoursRequest_returns200() {
        jdbcTemplate.execute(
                "INSERT INTO project (id, name, status, total_budget_hours, consumed_hours, pm_id, version) "
                        + "VALUES (500, 'Review Project', 'ACTIVE', 200.0, 0.0, " + PM_USER_ID + ", 0)");
        jdbcTemplate.execute(
                "INSERT INTO hours_request (id, project_id, requester_id, requested_hours, description, "
                        + "target_type, status, created_at, updated_at) "
                        + "VALUES (600, 500, " + PM_USER_ID + ", 50.0, '需要增補', 'PROJECT', 'PENDING', NOW(), NOW())");

        var review = new ReviewHoursRequestRequest(HoursRequestStatus.APPROVED, "核准");
        ResponseEntity<HoursRequestResponse> response = doPost("/api/admin/hours-requests/600/review", review, HoursRequestResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(HoursRequestStatus.APPROVED);
    }

    @Test
    void rejectHoursRequest_returns200() {
        jdbcTemplate.execute(
                "INSERT INTO project (id, name, status, total_budget_hours, consumed_hours, pm_id, version) "
                        + "VALUES (501, 'Reject Project', 'ACTIVE', 200.0, 0.0, " + PM_USER_ID + ", 0)");
        jdbcTemplate.execute(
                "INSERT INTO hours_request (id, project_id, requester_id, requested_hours, description, "
                        + "target_type, status, created_at, updated_at) "
                        + "VALUES (601, 501, " + PM_USER_ID + ", 30.0, '需要增補', 'PROJECT', 'PENDING', NOW(), NOW())");

        var review = new ReviewHoursRequestRequest(HoursRequestStatus.REJECTED, "預算不足");
        ResponseEntity<HoursRequestResponse> response = doPost("/api/admin/hours-requests/601/review", review, HoursRequestResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(HoursRequestStatus.REJECTED);
    }

    @Test
    void listAllHoursRequests_returns200() {
        ResponseEntity<Map> response = doGet("/api/admin/hours-requests", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("content")).isNotNull();
    }
}
