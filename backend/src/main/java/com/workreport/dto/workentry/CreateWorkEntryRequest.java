package com.workreport.dto.workentry;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateWorkEntryRequest(
        @NotNull Long taskId,
        @NotNull LocalDate workDate,
        @NotNull @DecimalMin("0.5") @DecimalMax("24.0") BigDecimal hours
) {}
