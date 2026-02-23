package com.workreport.dto.hoursrequest;

import com.workreport.enums.HoursRequestStatus;
import com.workreport.enums.HoursRequestTargetType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HoursRequestResponse(
        Long id,
        Long projectId,
        String projectName,
        Long requesterId,
        String requesterName,
        BigDecimal requestedHours,
        String description,
        HoursRequestTargetType targetType,
        Long targetTaskId,
        String targetTaskName,
        HoursRequestStatus status,
        Long reviewerId,
        String reviewerName,
        String reviewComment,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
) {}
