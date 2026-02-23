package com.workreport.dto.hoursrequest;

import com.workreport.enums.HoursRequestTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateHoursRequestRequest(
        @NotNull Long projectId,
        @NotNull @Positive BigDecimal requestedHours,
        @NotBlank String description,
        @NotNull HoursRequestTargetType targetType,
        Long targetTaskId
) {}
