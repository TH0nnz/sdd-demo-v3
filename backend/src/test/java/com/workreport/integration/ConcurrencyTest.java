package com.workreport.integration;

import com.workreport.entity.Project;
import com.workreport.enums.Role;
import com.workreport.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency integration tests:
 * 1. Multiple threads simultaneously filing work entries for the same task
 *    → verifies @Version optimistic locking and data consistency.
 * 2. Multiple threads simultaneously updating the same project budget
 *    → verifies exactly one commit succeeds while others get OptimisticLockException.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class ConcurrencyTest {

    private static final long TEST_PROJECT_ID = 100L;
    private static final long TEST_TASK_ID = 100L;
    private static final long EXECUTOR_USER_ID = 4L;
    private static final long PM_USER_ID = 2L;

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private RestClient restClient;
    private String executorToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM work_entry");
        jdbcTemplate.execute("DELETE FROM hours_request");
        jdbcTemplate.execute("DELETE FROM task WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM project WHERE id > 0");

        jdbcTemplate.execute(
                "INSERT INTO project (id, name, status, total_budget_hours, consumed_hours, pm_id, version) "
                        + "VALUES (100, 'Concurrency Project', 'ACTIVE', 1000.0, 0.0, "
                        + PM_USER_ID + ", 0)");

        jdbcTemplate.execute(
                "INSERT INTO task (id, name, project_id, status, budget_hours, consumed_hours, assignee_id, version) "
                        + "VALUES (100, 'Concurrent Task', 100, 'IN_PROGRESS', 200.0, 0.0, "
                        + EXECUTOR_USER_ID + ", 0)");

        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
        executorToken = jwtTokenProvider.generateToken(
                EXECUTOR_USER_ID, "executor@company.com", Set.of(Role.EXECUTOR));
    }

    // ─── Test 1: concurrent work-entry creation via HTTP ───

    @Test
    void concurrentWorkEntryCreation_shouldMaintainDataConsistency() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        String today = LocalDate.now().toString();
        String body = String.format(
                "{\"taskId\":%d,\"workDate\":\"%s\",\"hours\":1.0}", TEST_TASK_ID, today);

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    startLatch.await();
                    int status = post(executorToken, "/api/work-entries", body);
                    if (status >= 200 && status < 300) {
                        successes.incrementAndGet();
                    } else {
                        failures.incrementAndGet();
                    }
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release all threads at once
        startLatch.countDown();
        assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        // At least one request must succeed
        assertThat(successes.get()).isGreaterThanOrEqualTo(1);

        // All requests accounted for
        assertThat(successes.get() + failures.get()).isEqualTo(threadCount);

        // Data consistency: consumed hours on the task should be > 0
        BigDecimal consumed = jdbcTemplate.queryForObject(
                "SELECT consumed_hours FROM task WHERE id = ?", BigDecimal.class, TEST_TASK_ID);
        assertThat(consumed).isGreaterThan(BigDecimal.ZERO);
    }

    // ─── Test 2: concurrent project update with optimistic locking ───

    @Test
    void concurrentProjectUpdate_shouldDetectOptimisticLock() throws InterruptedException {
        int threadCount = 5;
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger lockFailures = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            final BigDecimal newBudget = new BigDecimal(500 + i * 100);
            pool.submit(() -> {
                EntityManager em = entityManagerFactory.createEntityManager();
                EntityTransaction tx = em.getTransaction();
                try {
                    tx.begin();
                    // All threads load project at version 0
                    Project project = em.find(Project.class, TEST_PROJECT_ID);
                    readyLatch.countDown();
                    startLatch.await(); // wait for all threads to have loaded

                    project.setTotalBudgetHours(newBudget);
                    tx.commit(); // flush triggers UPDATE … WHERE version = 0
                    successes.incrementAndGet();
                } catch (Exception e) {
                    if (tx.isActive()) {
                        tx.rollback();
                    }
                    if (isOptimisticLockException(e)) {
                        lockFailures.incrementAndGet();
                    }
                } finally {
                    em.close();
                }
            });
        }

        // Wait until every thread has read the entity
        assertThat(readyLatch.await(10, TimeUnit.SECONDS)).isTrue();
        // Release them all
        startLatch.countDown();

        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // Exactly one thread can win the version race
        assertThat(successes.get()).isEqualTo(1);
        assertThat(lockFailures.get()).isEqualTo(threadCount - 1);
    }

    // ─── Helpers ───

    private int post(String token, String uri, String requestBody) {
        try {
            return restClient.post()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode().value();
        } catch (RestClientResponseException e) {
            return e.getStatusCode().value();
        }
    }

    private static boolean isOptimisticLockException(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String name = t.getClass().getName();
            if (name.contains("OptimisticLock") || name.contains("StaleState") || name.contains("StaleObject")) {
                return true;
            }
        }
        return false;
    }
}
