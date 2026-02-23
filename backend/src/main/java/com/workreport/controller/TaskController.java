package com.workreport.controller;

import com.workreport.dto.common.PageResponse;
import com.workreport.dto.task.CreateTaskRequest;
import com.workreport.dto.task.TaskResponse;
import com.workreport.dto.task.UpdateTaskRequest;
import com.workreport.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@PreAuthorize("hasAnyRole('PM', 'ADMIN')")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<TaskResponse>> listTasks(
            @PathVariable Long projectId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(taskService.getTasks(projectId, pageable));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest request) {
        Long userId = getCurrentUserId();
        TaskResponse response = taskService.createTask(userId, projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(taskService.updateTask(userId, projectId, id, request));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<TaskResponse> closeTask(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(taskService.closeTask(userId, projectId, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        Long userId = getCurrentUserId();
        taskService.deleteTask(userId, projectId, id);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
