package com.workreport.dto.department;

import java.math.BigDecimal;

public record MemberSummaryDto(
        Long userId,
        String name,
        BigDecimal totalHoursThisMonth,
        BigDecimal todayHours
) {
}
