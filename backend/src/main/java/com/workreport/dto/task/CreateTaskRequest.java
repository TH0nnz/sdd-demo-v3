package com.workreport.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateTaskRequest(
        @NotBlank String name,
        @NotNull @Positive BigDecimal budgetHours,
        Long assigneeId
) {}
