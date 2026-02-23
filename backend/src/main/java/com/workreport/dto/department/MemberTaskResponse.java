package com.workreport.dto.department;

import com.workreport.enums.TaskStatus;

import java.math.BigDecimal;

public record MemberTaskResponse(
        Long taskId,
        String taskName,
        String projectName,
        TaskStatus status,
        BigDecimal consumedHours
) {
}
