package com.workreport.service;

import com.workreport.dto.common.PageResponse;
import com.workreport.dto.task.TaskResponse;
import com.workreport.entity.Task;
import com.workreport.enums.NotificationType;
import com.workreport.enums.TaskStatus;
import com.workreport.exception.BusinessRuleException;
import com.workreport.exception.ResourceNotFoundException;
import com.workreport.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ExecutorTaskService {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    public ExecutorTaskService(TaskRepository taskRepository,
                               NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getMyTasks(Long userId, TaskStatus statusFilter, Pageable pageable) {
        Page<Task> page;
        if (statusFilter != null) {
            page = taskRepository.findByAssigneeIdAndStatusIn(userId, List.of(statusFilter), pageable);
        } else {
            page = taskRepository.findByAssigneeId(userId, pageable);
        }
        return PageResponse.from(page.map(this::toResponse));
    }

    public TaskResponse completeTask(Long userId, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        // Validate task assigned to user
        if (task.getAssignee() == null || !task.getAssignee().getId().equals(userId)) {
            throw new BusinessRuleException("Task is not assigned to you", HttpStatus.FORBIDDEN);
        }

        // Validate task is IN_PROGRESS
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Only IN_PROGRESS tasks can be completed");
        }

        task.setStatus(TaskStatus.COMPLETED);
        taskRepository.save(task);

        // Notify PM
        Long pmId = task.getProject().getPm().getId();
        notificationService.notify(pmId, NotificationType.TASK_COMPLETED,
                "Task completed",
                "Task '" + task.getName() + "' in project '" + task.getProject().getName()
                        + "' has been marked as completed.");

        return toResponse(task);
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getName(),
                task.getProject().getId(),
                task.getProject().getName(),
                task.getStatus(),
                task.getBudgetHours(),
                task.getConsumedHours(),
                task.getBudgetHours().subtract(task.getConsumedHours()),
                task.getAssignee() != null ? task.getAssignee().getId() : null,
                task.getAssignee() != null ? task.getAssignee().getName() : null,
                task.getCreatedAt()
        );
    }
}
