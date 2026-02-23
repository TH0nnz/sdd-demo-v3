package com.workreport.controller;

import com.workreport.dto.common.PageResponse;
import com.workreport.dto.task.TaskResponse;
import com.workreport.enums.TaskStatus;
import com.workreport.service.ExecutorTaskService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/my-tasks")
@PreAuthorize("hasRole('EXECUTOR')")
public class ExecutorTaskController {

    private final ExecutorTaskService executorTaskService;

    public ExecutorTaskController(ExecutorTaskService executorTaskService) {
        this.executorTaskService = executorTaskService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<TaskResponse>> getMyTasks(
            @RequestParam(required = false) TaskStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(executorTaskService.getMyTasks(userId, status, pageable));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<TaskResponse> completeTask(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(executorTaskService.completeTask(userId, id));
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
