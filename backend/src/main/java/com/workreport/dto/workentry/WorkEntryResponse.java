package com.workreport.dto.workentry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkEntryResponse(
        Long id,
        Long taskId,
        String taskName,
        String projectName,
        LocalDate workDate,
        BigDecimal hours,
        boolean editable,
        BigDecimal taskRemainingHours,
        String warning,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
