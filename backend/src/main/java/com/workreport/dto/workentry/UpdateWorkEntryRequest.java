package com.workreport.dto.workentry;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateWorkEntryRequest(
        @NotNull @DecimalMin("0.5") @DecimalMax("24.0") BigDecimal hours
) {}
