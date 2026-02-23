package com.workreport.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateProjectRequest(
        @NotBlank String name,
        @NotNull @Positive BigDecimal totalBudgetHours,
        @NotNull Long pmId
) {}
