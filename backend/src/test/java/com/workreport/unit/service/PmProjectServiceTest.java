package com.workreport.unit.service;

import com.workreport.dto.common.PageResponse;
import com.workreport.dto.project.ProjectDashboardResponse;
import com.workreport.dto.project.ProjectDashboardResponse.TaskSummaryDto;
import com.workreport.entity.Project;
import com.workreport.enums.ProjectStatus;
import com.workreport.enums.TaskStatus;
import com.workreport.repository.ProjectRepository;
import com.workreport.repository.TaskRepository;
import com.workreport.service.PmProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PmProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private PmProjectService pmProjectService;

    private static final Long PM_USER_ID = 100L;

    @Nested
    @DisplayName("getMyProjects")
    class GetMyProjects {

        @Nested
        @DisplayName("有專案時")
        class WhenHasProjects {

            private Project project;
            private Pageable pageable;

            @BeforeEach
            void setUp() {
                pageable = PageRequest.of(0, 10);
                project = new Project();
                project.setId(1L);
                project.setName("Test Project");
                project.setStatus(ProjectStatus.ACTIVE);
                project.setTotalBudgetHours(new BigDecimal("100.0"));
                project.setConsumedHours(new BigDecimal("25.0"));
            }

            @Test
            @DisplayName("回傳分頁並驗證 ProjectDashboardResponse 欄位與 taskSummary 各狀態數量")
            void returnsPageWithProjectDashboardResponseAndTaskSummary() {
                when(projectRepository.findByPmId(eq(PM_USER_ID), eq(pageable)))
                        .thenReturn(new PageImpl<>(List.of(project), pageable, 1));
                when(taskRepository.countByProjectIdGroupByStatus(1L))
                        .thenReturn(List.<Object[]>of(
                                new Object[]{TaskStatus.PENDING, 2L},
                                new Object[]{TaskStatus.IN_PROGRESS, 3L},
                                new Object[]{TaskStatus.COMPLETED, 1L},
                                new Object[]{TaskStatus.CLOSED, 1L}
                        ));
                when(taskRepository.sumHoursByProjectId(1L))
                        .thenReturn(List.of(new Object[]{new BigDecimal("60.0"), new BigDecimal("15.0")}));

                PageResponse<ProjectDashboardResponse> result =
                        pmProjectService.getMyProjects(PM_USER_ID, pageable);

                assertThat(result.content()).hasSize(1);
                assertThat(result.page()).isZero();
                assertThat(result.size()).isEqualTo(10);
                assertThat(result.totalElements()).isEqualTo(1);
                assertThat(result.totalPages()).isEqualTo(1);

                ProjectDashboardResponse dto = result.content().get(0);
                assertThat(dto.id()).isEqualTo(1L);
                assertThat(dto.name()).isEqualTo("Test Project");
                assertThat(dto.status()).isEqualTo(ProjectStatus.ACTIVE);
                assertThat(dto.totalBudgetHours()).isEqualByComparingTo("100.0");
                assertThat(dto.consumedHours()).isEqualByComparingTo("25.0");
                assertThat(dto.remainingHours()).isEqualByComparingTo("75.0");
                assertThat(dto.usageRate()).isEqualByComparingTo("25.0");
                assertThat(dto.unallocatedHours()).isEqualByComparingTo("40.0");
                assertThat(dto.allocatedQuotaHours()).isEqualByComparingTo("60.0");
                assertThat(dto.allocatedConsumedHours()).isEqualByComparingTo("15.0");
                assertThat(dto.allocatedRemainingHours()).isEqualByComparingTo("45.0");

                TaskSummaryDto taskSummary = dto.taskSummary();
                assertThat(taskSummary.total()).isEqualTo(7);
                assertThat(taskSummary.pending()).isEqualTo(2);
                assertThat(taskSummary.inProgress()).isEqualTo(3);
                assertThat(taskSummary.completed()).isEqualTo(1);
                assertThat(taskSummary.closed()).isEqualTo(1);

                verify(projectRepository).findByPmId(PM_USER_ID, pageable);
                verify(taskRepository).countByProjectIdGroupByStatus(1L);
                verify(taskRepository).sumHoursByProjectId(1L);
            }

            @Test
            @DisplayName("totalBudgetHours 為 0 時 usageRate 為 ZERO")
            void usageRateIsZeroWhenTotalBudgetHoursIsZero() {
                project.setTotalBudgetHours(BigDecimal.ZERO);
                project.setConsumedHours(new BigDecimal("10.0"));

                when(projectRepository.findByPmId(eq(PM_USER_ID), eq(pageable)))
                        .thenReturn(new PageImpl<>(List.of(project), pageable, 1));
                when(taskRepository.countByProjectIdGroupByStatus(1L)).thenReturn(List.of());
                when(taskRepository.sumHoursByProjectId(1L)).thenReturn(List.of());

                PageResponse<ProjectDashboardResponse> result =
                        pmProjectService.getMyProjects(PM_USER_ID, pageable);

                ProjectDashboardResponse dto = result.content().get(0);
                assertThat(dto.totalBudgetHours()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(dto.consumedHours()).isEqualByComparingTo("10.0");
                assertThat(dto.usageRate()).isEqualByComparingTo(BigDecimal.ZERO);
            }

            @Test
            @DisplayName("taskSummary 缺少某狀態時以 0 計")
            void taskSummaryDefaultsMissingStatusToZero() {
                when(projectRepository.findByPmId(eq(PM_USER_ID), eq(pageable)))
                        .thenReturn(new PageImpl<>(List.of(project), pageable, 1));
                when(taskRepository.countByProjectIdGroupByStatus(1L))
                        .thenReturn(List.<Object[]>of(
                                new Object[]{TaskStatus.IN_PROGRESS, 5L}
                        ));
                when(taskRepository.sumHoursByProjectId(1L)).thenReturn(List.of(new Object[]{new BigDecimal("50.0"), new BigDecimal("10.0")}));

                PageResponse<ProjectDashboardResponse> result =
                        pmProjectService.getMyProjects(PM_USER_ID, pageable);

                TaskSummaryDto taskSummary = result.content().get(0).taskSummary();
                assertThat(taskSummary.total()).isEqualTo(5);
                assertThat(taskSummary.pending()).isEqualTo(0);
                assertThat(taskSummary.inProgress()).isEqualTo(5);
                assertThat(taskSummary.completed()).isEqualTo(0);
                assertThat(taskSummary.closed()).isEqualTo(0);
            }
        }

        @Nested
        @DisplayName("空頁時")
        class WhenEmptyPage {

            @Test
            @DisplayName("回傳空 content 與正確分頁資訊")
            void returnsEmptyContentAndCorrectPagination() {
                Pageable pageable = PageRequest.of(0, 10);
                when(projectRepository.findByPmId(eq(PM_USER_ID), eq(pageable)))
                        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

                PageResponse<ProjectDashboardResponse> result =
                        pmProjectService.getMyProjects(PM_USER_ID, pageable);

                assertThat(result.content()).isEmpty();
                assertThat(result.page()).isZero();
                assertThat(result.size()).isEqualTo(10);
                assertThat(result.totalElements()).isZero();
                assertThat(result.totalPages()).isZero();
            }
        }
    }
}
