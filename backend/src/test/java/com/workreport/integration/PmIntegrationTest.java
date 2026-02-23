package com.workreport.integration;

import com.workreport.dto.hoursrequest.CreateHoursRequestRequest;
import com.workreport.dto.hoursrequest.HoursRequestResponse;
import com.workreport.dto.task.CreateTaskRequest;
import com.workreport.dto.task.TaskResponse;
import com.workreport.dto.task.UpdateTaskRequest;
import com.workreport.enums.HoursRequestTargetType;
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
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class PmIntegrationTest {

    private static final Long PM_USER_ID = 2L;
    private static final Long EXECUTOR_USER_ID = 4L;

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RestClient restClient;
    private String pmToken;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (req, resp) -> {})
                .build();
        pmToken = jwtTokenProvider.generateToken(PM_USER_ID, "pm@company.com", Set.of(Role.PM));

        jdbcTemplate.execute("DELETE FROM hours_request");
        jdbcTemplate.execute("DELETE FROM work_entry");
        jdbcTemplate.execute("DELETE FROM task WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM project WHERE id > 0");

        jdbcTemplate.execute(
                "INSERT INTO project (id, name, status, total_budget_hours, consumed_hours, pm_id, version) "
                        + "VALUES (100, 'PM Test Project', 'ACTIVE', 500.0, 50.0, " + PM_USER_ID + ", 0)");
    }

    private <T> ResponseEntity<T> doPost(String uri, Object body, Class<T> type) {
        return restClient.post().uri(uri)
                .headers(h -> { h.setBearerAuth(pmToken); h.setContentType(MediaType.APPLICATION_JSON); })
                .body(body)
                .retrieve()
                .toEntity(type);
    }

    private <T> ResponseEntity<T> doGet(String uri, Class<T> type) {
        return restClient.get().uri(uri)
                .headers(h -> h.setBearerAuth(pmToken))
                .retrieve()
                .toEntity(type);
    }

    private <T> ResponseEntity<T> doPut(String uri, Object body, Class<T> type) {
        return restClient.put().uri(uri)
                .headers(h -> { h.setBearerAuth(pmToken); h.setContentType(MediaType.APPLICATION_JSON); })
                .body(body)
                .retrieve()
                .toEntity(type);
    }

    private ResponseEntity<Void> doDelete(String uri) {
        return restClient.delete().uri(uri)
                .headers(h -> h.setBearerAuth(pmToken))
                .retrieve()
                .toBodilessEntity();
    }

    // --- Task CRUD ---

    @Test
    void createTask_returns201() {
        var request = new CreateTaskRequest("New Task", new BigDecimal("20.0"), EXECUTOR_USER_ID);
        ResponseEntity<TaskResponse> response = doPost("/api/projects/100/tasks", request, TaskResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("New Task");
        assertThat(response.getBody().status()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void listTasks_returns200() {
        doPost("/api/projects/100/tasks",
                new CreateTaskRequest("List Test Task", new BigDecimal("10.0"), null), TaskResponse.class);

        ResponseEntity<Map> response = doGet("/api/projects/100/tasks", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("content")).isNotNull();
    }

    @Test
    void updateTask_returns200() {
        ResponseEntity<TaskResponse> createResp = doPost("/api/projects/100/tasks",
                new CreateTaskRequest("Original Task", new BigDecimal("10.0"), null), TaskResponse.class);
        Long taskId = createResp.getBody().id();

        var updateReq = new UpdateTaskRequest("Updated Task", new BigDecimal("30.0"), EXECUTOR_USER_ID);
        ResponseEntity<TaskResponse> response = doPut("/api/projects/100/tasks/" + taskId, updateReq, TaskResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Updated Task");
    }

    @Test
    void closeTask_returns200() {
        ResponseEntity<TaskResponse> createResp = doPost("/api/projects/100/tasks",
                new CreateTaskRequest("Task To Close", new BigDecimal("15.0"), null), TaskResponse.class);
        Long taskId = createResp.getBody().id();

        ResponseEntity<TaskResponse> response = doPost("/api/projects/100/tasks/" + taskId + "/close", "", TaskResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(TaskStatus.CLOSED);
    }

    @Test
    void deleteTask_returns204() {
        ResponseEntity<TaskResponse> createResp = doPost("/api/projects/100/tasks",
                new CreateTaskRequest("Task To Delete", new BigDecimal("5.0"), null), TaskResponse.class);
        Long taskId = createResp.getBody().id();

        ResponseEntity<Void> response = doDelete("/api/projects/100/tasks/" + taskId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // --- Dashboard ---

    @Test
    void getDashboard_returns200() {
        doPost("/api/projects/100/tasks",
                new CreateTaskRequest("Task A", new BigDecimal("10.0"), null), TaskResponse.class);

        ResponseEntity<Map> response = doGet("/api/pm/projects", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("content")).isNotNull();
    }

    // --- Hours Request ---

    @Test
    void createHoursRequest_returns201() {
        var request = new CreateHoursRequestRequest(
                100L, new BigDecimal("40.0"), "前端工作量超出預期",
                HoursRequestTargetType.PROJECT, null);

        ResponseEntity<HoursRequestResponse> response = doPost("/api/hours-requests", request, HoursRequestResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().projectId()).isEqualTo(100L);
        assertThat(response.getBody().requestedHours()).isEqualByComparingTo(new BigDecimal("40.0"));
    }
}
