package com.workreport.controller;

import com.workreport.dto.common.PageResponse;
import com.workreport.dto.project.ProjectDashboardResponse;
import com.workreport.service.PmProjectService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pm/projects")
@PreAuthorize("hasRole('PM')")
public class PmProjectController {

    private final PmProjectService pmProjectService;

    public PmProjectController(PmProjectService pmProjectService) {
        this.pmProjectService = pmProjectService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProjectDashboardResponse>> getMyProjects(
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(pmProjectService.getMyProjects(userId, pageable));
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
