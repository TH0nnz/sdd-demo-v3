package com.workreport.controller;

import com.workreport.dto.common.PageResponse;
import com.workreport.dto.hoursrequest.HoursRequestResponse;
import com.workreport.dto.hoursrequest.ReviewHoursRequestRequest;
import com.workreport.service.HoursRequestReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/hours-requests")
@PreAuthorize("hasRole('ADMIN')")
public class HoursRequestReviewController {

    private final HoursRequestReviewService hoursRequestReviewService;

    public HoursRequestReviewController(HoursRequestReviewService hoursRequestReviewService) {
        this.hoursRequestReviewService = hoursRequestReviewService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<HoursRequestResponse>> listAllRequests(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(hoursRequestReviewService.getAllRequests(pageable));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<HoursRequestResponse> reviewRequest(
            @PathVariable Long id,
            @Valid @RequestBody ReviewHoursRequestRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(hoursRequestReviewService.reviewRequest(userId, id, request));
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
