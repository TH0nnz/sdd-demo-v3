package com.workreport.integration;

import com.workreport.repository.ProjectRepository;
import com.workreport.repository.TaskRepository;
import com.workreport.repository.WorkEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Query performance test — verifies key repository queries execute within 500 ms
 * against a dataset of 5+ projects, 25 tasks, and 125+ work entries.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class QueryPerformanceTest {

    private static final Long TEST_USER_ID = 4L;   // executor
    private static final Long TEST_PM_ID = 2L;     // pm

    @Autowired
    private WorkEntryRepository workEntryRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM work_entry");
        jdbcTemplate.execute("DELETE FROM hours_request");
        jdbcTemplate.execute("DELETE FROM task WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM project WHERE id > 0");

        // 5 projects
        for (int i = 0; i < 5; i++) {
            jdbcTemplate.execute(String.format(
                    "INSERT INTO project (id, name, status, total_budget_hours, consumed_hours, pm_id, version) "
                            + "VALUES (%d, 'Perf Project %d', 'ACTIVE', 2000, 0, %d, 0)",
                    200 + i, i, TEST_PM_ID));
        }

        // 25 tasks (5 per project)
        for (int i = 0; i < 25; i++) {
            long projectId = 200 + (i / 5);
            jdbcTemplate.execute(String.format(
                    "INSERT INTO task (id, name, project_id, status, budget_hours, consumed_hours, assignee_id, version) "
                            + "VALUES (%d, 'Perf Task %d', %d, 'IN_PROGRESS', 200, 0, %d, 0)",
                    200 + i, i, projectId, TEST_USER_ID));
        }

        // 125 work entries (5 per task, spread across January 2026)
        StringBuilder sb = new StringBuilder(
                "INSERT INTO work_entry (user_id, task_id, work_date, hours, created_at, updated_at) VALUES ");
        for (int i = 0; i < 125; i++) {
            long taskId = 200 + (i / 5);
            LocalDate date = LocalDate.of(2026, 1, 1).plusDays(i % 28);
            if (i > 0) sb.append(',');
            sb.append(String.format("(%d, %d, '%s', 4.0, NOW(), NOW())", TEST_USER_ID, taskId, date));
        }
        jdbcTemplate.execute(sb.toString());
    }

    @Test
    void findByUserIdAndWorkDateBetween_shouldCompleteWithin500ms() {
        long start = System.nanoTime();
        var results = workEntryRepository.findByUserIdAndWorkDateBetween(
                TEST_USER_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(results).isNotEmpty();
        assertThat(elapsedMs).isLessThan(500);
    }

    @Test
    void findByAssigneeId_shouldCompleteWithin500ms() {
        long start = System.nanoTime();
        var page = taskRepository.findByAssigneeId(TEST_USER_ID, PageRequest.of(0, 20));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(page.getContent()).isNotEmpty();
        assertThat(elapsedMs).isLessThan(500);
    }

    @Test
    void sumHoursByUserIdAndWorkDate_shouldCompleteWithin500ms() {
        long start = System.nanoTime();
        var sum = workEntryRepository.sumHoursByUserIdAndWorkDate(
                TEST_USER_ID, LocalDate.of(2026, 1, 15));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(sum).isNotNull();
        assertThat(elapsedMs).isLessThan(500);
    }

    @Test
    void findByPmId_shouldCompleteWithin500ms() {
        long start = System.nanoTime();
        var projects = projectRepository.findByPmId(TEST_PM_ID);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(projects).isNotEmpty();
        assertThat(elapsedMs).isLessThan(500);
    }
}
