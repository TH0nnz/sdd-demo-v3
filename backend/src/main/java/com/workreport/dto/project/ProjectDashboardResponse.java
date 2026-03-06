package com.workreport.dto.project;

import com.workreport.enums.ProjectStatus;

import java.math.BigDecimal;

public record ProjectDashboardResponse(
        Long id,
        String name,
        ProjectStatus status,
        BigDecimal totalBudgetHours,
        BigDecimal consumedHours,
        BigDecimal remainingHours,
        BigDecimal usageRate,
        BigDecimal unallocatedHours,
        BigDecimal allocatedQuotaHours,
        BigDecimal allocatedConsumedHours,
        BigDecimal allocatedRemainingHours,
        TaskSummaryDto taskSummary
) {

    public record TaskSummaryDto(
            long total,
            long pending,
            long inProgress,
            long completed,
            long closed
    ) {}
}
