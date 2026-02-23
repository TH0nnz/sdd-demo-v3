package com.workreport.dto.task;

import com.workreport.enums.TaskStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String name,
        Long projectId,
        String projectName,
        TaskStatus status,
        BigDecimal budgetHours,
        BigDecimal consumedHours,
        BigDecimal remainingHours,
        Long assigneeId,
        String assigneeName,
        LocalDateTime createdAt
) {}
