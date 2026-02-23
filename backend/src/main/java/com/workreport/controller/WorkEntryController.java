package com.workreport.controller;

import com.workreport.dto.common.PageResponse;
import com.workreport.dto.workentry.CreateWorkEntryRequest;
import com.workreport.dto.workentry.UpdateWorkEntryRequest;
import com.workreport.dto.workentry.WorkEntryResponse;
import com.workreport.service.WorkEntryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/work-entries")
@PreAuthorize("hasRole('EXECUTOR')")
public class WorkEntryController {

    private final WorkEntryService workEntryService;

    public WorkEntryController(WorkEntryService workEntryService) {
        this.workEntryService = workEntryService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<WorkEntryResponse>> getWorkEntries(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = getCurrentUserId();
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        return ResponseEntity.ok(workEntryService.getWorkEntries(userId, start, end, pageable));
    }

    @PostMapping
    public ResponseEntity<WorkEntryResponse> createWorkEntry(
            @Valid @RequestBody CreateWorkEntryRequest request) {
        Long userId = getCurrentUserId();
        WorkEntryResponse response = workEntryService.createWorkEntry(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkEntryResponse> updateWorkEntry(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkEntryRequest request) {
        Long userId = getCurrentUserId();
        WorkEntryResponse response = workEntryService.updateWorkEntry(userId, id, request);
        return ResponseEntity.ok(response);
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
