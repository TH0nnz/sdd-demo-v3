package com.workreport.dto.hoursrequest;

import com.workreport.enums.HoursRequestStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewHoursRequestRequest(
        @NotNull HoursRequestStatus decision,
        String reviewNote
) {}
