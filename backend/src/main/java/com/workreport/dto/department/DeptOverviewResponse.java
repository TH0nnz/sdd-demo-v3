package com.workreport.dto.department;

import java.math.BigDecimal;
import java.util.List;

public record DeptOverviewResponse(
        String deptName,
        int memberCount,
        BigDecimal totalHoursThisWeek,
        BigDecimal totalHoursThisMonth,
        List<MemberSummaryDto> members
) {
}
