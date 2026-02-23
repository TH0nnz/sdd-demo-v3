package com.workreport.integration;

import com.workreport.dto.workentry.CreateWorkEntryRequest;
import com.workreport.dto.workentry.UpdateWorkEntryRequest;
import com.workreport.dto.workentry.WorkEntryResponse;
import com.workreport.enums.Role;
import com.workreport.enums.TaskStatus;
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
import org.springframework.web.client.RestClientResponseException;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class WorkEntryIntegrationTest {

    private static final Long EXECUTOR_USER_ID = 4L;
    private static final Long PM_USER_ID = 2L;

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RestClient restClient;
    private String executorToken;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (req, resp) -> {})
                .build();
        executorToken = jwtTokenProvider.generateToken(EXECUTOR_USER_ID, "executor@company.com",
                Set.of(Role.EXECUTOR));

        jdbcTemplate.execute("DELETE FROM work_entry");
        jdbcTemplate.execute("DELETE FROM hours_request");
        jdbcTemplate.execute("DELETE FROM task WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM project WHERE id > 0");

        jdbcTemplate.execute(
                "INSERT INTO project (id, name, status, total_budget_hours, consumed_hours, pm_id, version) "
                        + "VALUES (100, 'Integration Test Project', 'ACTIVE', 500.0, 0.0, " + PM_USER_ID + ", 0)");

        jdbcTemplate.execute(
                "INSERT INTO task (id, name, project_id, status, budget_hours, consumed_hours, assignee_id, version) "
                        + "VALUES (100, 'Active Task', 100, 'IN_PROGRESS', 100.0, 10.0, " + EXECUTOR_USER_ID + ", 0)");
        jdbcTemplate.execute(
                "INSERT INTO task (id, name, project_id, status, budget_hours, consumed_hours, assignee_id, version) "
                        + "VALUES (101, 'Completed Task', 100, 'COMPLETED', 50.0, 50.0, " + EXECUTOR_USER_ID + ", 0)");
        jdbcTemplate.execute(
                "INSERT INTO task (id, name, project_id, status, budget_hours, consumed_hours, assignee_id, version) "
                        + "VALUES (102, 'Other User Task', 100, 'IN_PROGRESS', 80.0, 0.0, " + PM_USER_ID + ", 0)");
        jdbcTemplate.execute(
                "INSERT INTO task (id, name, project_id, status, budget_hours, consumed_hours, assignee_id, version) "
                        + "VALUES (103, 'Pending Task', 100, 'PENDING', 100.0, 0.0, " + EXECUTOR_USER_ID + ", 0)");
    }

    private <T> ResponseEntity<T> doPost(String uri, Object body, Class<T> type) {
        return restClient.post().uri(uri)
                .headers(h -> { h.setBearerAuth(executorToken); h.setContentType(MediaType.APPLICATION_JSON); })
                .body(body)
                .retrieve()
                .toEntity(type);
    }

    private <T> ResponseEntity<T> doGet(String uri, Class<T> type) {
        return restClient.get().uri(uri)
                .headers(h -> h.setBearerAuth(executorToken))
                .retrieve()
                .toEntity(type);
    }

    private <T> ResponseEntity<T> doPut(String uri, Object body, Class<T> type) {
        return restClient.put().uri(uri)
                .headers(h -> { h.setBearerAuth(executorToken); h.setContentType(MediaType.APPLICATION_JSON); })
                .body(body)
                .retrieve()
                .toEntity(type);
    }

    @Test
    void createWorkEntry_returns201() {
        LocalDate today = LocalDate.now();
        var request = new CreateWorkEntryRequest(100L, today, new BigDecimal("2.0"));

        ResponseEntity<WorkEntryResponse> response = doPost("/api/work-entries", request, WorkEntryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().taskId()).isEqualTo(100L);
        assertThat(response.getBody().hours()).isEqualByComparingTo(new BigDecimal("2.0"));
    }

    @Test
    void updateWorkEntry_returns200() {
        LocalDate today = LocalDate.now();
        var createReq = new CreateWorkEntryRequest(100L, today, new BigDecimal("2.0"));
        ResponseEntity<WorkEntryResponse> createResp = doPost("/api/work-entries", createReq, WorkEntryResponse.class);
        Long entryId = createResp.getBody().id();

        var updateReq = new UpdateWorkEntryRequest(new BigDecimal("3.0"));
        ResponseEntity<WorkEntryResponse> response = doPut("/api/work-entries/" + entryId, updateReq, WorkEntryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().hours()).isEqualByComparingTo(new BigDecimal("3.0"));
    }

    @Test
    void getWorkEntries_returns200() {
        LocalDate today = LocalDate.now();
        doPost("/api/work-entries", new CreateWorkEntryRequest(100L, today, new BigDecimal("1.5")), WorkEntryResponse.class);

        ResponseEntity<Map> response = doGet(
                "/api/work-entries?startDate=" + today.minusDays(7) + "&endDate=" + today, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("content")).isNotNull();
    }

    @Test
    void getMyTasks_returns200() {
        ResponseEntity<Map> response = doGet("/api/my-tasks", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("content")).isNotNull();
    }

    @Test
    void completeTask_returns200() {
        LocalDate today = LocalDate.now();
        doPost("/api/work-entries", new CreateWorkEntryRequest(103L, today, new BigDecimal("1.0")), WorkEntryResponse.class);

        ResponseEntity<Map> response = doPost("/api/my-tasks/103/complete", "", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void createWorkEntry_terminalTask_returns400() {
        LocalDate today = LocalDate.now();
        var request = new CreateWorkEntryRequest(101L, today, new BigDecimal("1.0"));

        ResponseEntity<Map> response = doPost("/api/work-entries", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createWorkEntry_notAssignedTask_returns403() {
        LocalDate today = LocalDate.now();
        var request = new CreateWorkEntryRequest(102L, today, new BigDecimal("1.0"));

        ResponseEntity<Map> response = doPost("/api/work-entries", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
