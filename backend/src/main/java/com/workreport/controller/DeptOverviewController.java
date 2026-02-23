package com.workreport.controller;

import com.workreport.dto.department.DepartmentResponse;
import com.workreport.dto.department.DeptOverviewResponse;
import com.workreport.repository.DepartmentRepository;
import com.workreport.service.DeptOverviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dept")
@PreAuthorize("hasRole('DEPT_MANAGER')")
public class DeptOverviewController {

    private final DeptOverviewService deptOverviewService;
    private final DepartmentRepository departmentRepository;

    public DeptOverviewController(DeptOverviewService deptOverviewService,
                                  DepartmentRepository departmentRepository) {
        this.deptOverviewService = deptOverviewService;
        this.departmentRepository = departmentRepository;
    }

    @GetMapping("/overview")
    public ResponseEntity<DeptOverviewResponse> getOverview() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(deptOverviewService.getDepartmentOverview(userId));
    }

    @GetMapping("/departments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DepartmentResponse>> listDepartments() {
        List<DepartmentResponse> departments = departmentRepository.findAll().stream()
                .map(DepartmentResponse::from)
                .toList();
        return ResponseEntity.ok(departments);
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
