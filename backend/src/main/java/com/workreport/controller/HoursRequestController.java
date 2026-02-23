package com.workreport.controller;

import com.workreport.dto.common.PageResponse;
import com.workreport.dto.hoursrequest.CreateHoursRequestRequest;
import com.workreport.dto.hoursrequest.HoursRequestResponse;
import com.workreport.service.HoursRequestService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hours-requests")
public class HoursRequestController {

    private final HoursRequestService hoursRequestService;

    public HoursRequestController(HoursRequestService hoursRequestService) {
        this.hoursRequestService = hoursRequestService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PM', 'ADMIN')")
    public ResponseEntity<PageResponse<HoursRequestResponse>> listRequests(
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(hoursRequestService.getRequests(userId, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('PM')")
    public ResponseEntity<HoursRequestResponse> createRequest(
            @Valid @RequestBody CreateHoursRequestRequest request) {
        Long userId = getCurrentUserId();
        HoursRequestResponse response = hoursRequestService.createRequest(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
