package com.workreport.dto.project;

import com.workreport.enums.ProjectStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        String name,
        ProjectStatus status,
        BigDecimal totalBudgetHours,
        BigDecimal consumedHours,
        Long pmId,
        String pmName,
        Long departmentId,
        String departmentName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime closedAt
) {}
