package com.workreport.service;

import com.workreport.annotation.RequireRole;
import com.workreport.dto.common.PageResponse;
import com.workreport.dto.project.ProjectDashboardResponse;
import com.workreport.dto.project.ProjectDashboardResponse.TaskSummaryDto;
import com.workreport.entity.Project;
import com.workreport.enums.Role;
import com.workreport.enums.TaskStatus;
import com.workreport.repository.ProjectRepository;
import com.workreport.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class PmProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public PmProjectService(ProjectRepository projectRepository,
                             TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    @RequireRole({Role.PM, Role.ADMIN})
    public PageResponse<ProjectDashboardResponse> getMyProjects(Long pmUserId, Pageable pageable) {
        Page<Project> page = projectRepository.findByPmId(pmUserId, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    private ProjectDashboardResponse toResponse(Project project) {
        BigDecimal remaining = project.getTotalBudgetHours().subtract(project.getConsumedHours());
        BigDecimal usageRate = BigDecimal.ZERO;
        if (project.getTotalBudgetHours().compareTo(BigDecimal.ZERO) > 0) {
            usageRate = project.getConsumedHours()
                    .multiply(new BigDecimal("100"))
                    .divide(project.getTotalBudgetHours(), 1, RoundingMode.HALF_UP);
        }

        TaskSummaryDto taskSummary = buildTaskSummary(project.getId());
        AllocatedHours allocated = buildAllocatedHours(project.getId());
        BigDecimal unallocated = project.getTotalBudgetHours().subtract(allocated.quota);

        return new ProjectDashboardResponse(
                project.getId(),
                project.getName(),
                project.getStatus(),
                project.getTotalBudgetHours(),
                project.getConsumedHours(),
                remaining,
                usageRate,
                unallocated,
                allocated.quota,
                allocated.consumed,
                allocated.remaining,
                taskSummary
        );
    }

    private static class AllocatedHours {
        final BigDecimal quota;
        final BigDecimal consumed;
        final BigDecimal remaining;

        AllocatedHours(BigDecimal quota, BigDecimal consumed, BigDecimal remaining) {
            this.quota = quota;
            this.consumed = consumed;
            this.remaining = remaining;
        }
    }

    private AllocatedHours buildAllocatedHours(Long projectId) {
        List<Object[]> rows = taskRepository.sumHoursByProjectId(projectId);
        if (rows == null || rows.isEmpty()) {
            return new AllocatedHours(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        Object[] row = rows.get(0);
        BigDecimal quota = row != null && row[0] != null ? toBigDecimal(row[0]) : BigDecimal.ZERO;
        BigDecimal consumed = row != null && row.length > 1 && row[1] != null ? toBigDecimal(row[1]) : BigDecimal.ZERO;
        BigDecimal remaining = quota.subtract(consumed);
        return new AllocatedHours(quota, consumed, remaining);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        return new BigDecimal(value.toString());
    }

    private TaskSummaryDto buildTaskSummary(Long projectId) {
        List<Object[]> statusCounts = taskRepository.countByProjectIdGroupByStatus(projectId);
        Map<TaskStatus, Long> countMap = new EnumMap<>(TaskStatus.class);
        long total = 0;
        for (Object[] row : statusCounts) {
            TaskStatus status = (TaskStatus) row[0];
            Long count = (Long) row[1];
            countMap.put(status, count);
            total += count;
        }

        return new TaskSummaryDto(
                total,
                countMap.getOrDefault(TaskStatus.PENDING, 0L),
                countMap.getOrDefault(TaskStatus.IN_PROGRESS, 0L),
                countMap.getOrDefault(TaskStatus.COMPLETED, 0L),
                countMap.getOrDefault(TaskStatus.CLOSED, 0L)
        );
    }
}
