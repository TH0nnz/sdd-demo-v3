package com.workreport.service;

import com.workreport.dto.common.PageResponse;
import com.workreport.dto.task.CreateTaskRequest;
import com.workreport.dto.task.TaskResponse;
import com.workreport.dto.task.UpdateTaskRequest;
import com.workreport.entity.Project;
import com.workreport.entity.Task;
import com.workreport.entity.User;
import com.workreport.enums.AuditActionType;
import com.workreport.enums.ProjectStatus;
import com.workreport.enums.Role;
import com.workreport.enums.TaskStatus;
import com.workreport.exception.BusinessRuleException;
import com.workreport.exception.ResourceNotFoundException;
import com.workreport.repository.ProjectRepository;
import com.workreport.repository.TaskRepository;
import com.workreport.repository.UserRepository;
import com.workreport.repository.WorkEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WorkEntryRepository workEntryRepository;
    private final AuditLogService auditLogService;

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       UserRepository userRepository,
                       WorkEntryRepository workEntryRepository,
                       AuditLogService auditLogService) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.workEntryRepository = workEntryRepository;
        this.auditLogService = auditLogService;
    }

    public TaskResponse createTask(Long pmUserId, Long projectId, CreateTaskRequest request) {
        Project project = getProjectAndValidateOwnership(pmUserId, projectId);

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new BusinessRuleException("專案已關閉，無法建立新 task");
        }

        User assignee = null;
        if (request.assigneeId() != null) {
            assignee = userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.assigneeId()));
            if (!assignee.getRoles().contains(Role.EXECUTOR)) {
                throw new BusinessRuleException("指定的執行人員無 EXECUTOR 角色");
            }
        }

        Task task = new Task();
        task.setName(request.name());
        task.setProject(project);
        task.setBudgetHours(request.budgetHours());
        task.setStatus(TaskStatus.PENDING);
        task.setAssignee(assignee);
        task = taskRepository.save(task);

        return toResponse(task);
    }

    public TaskResponse updateTask(Long pmUserId, Long projectId, Long taskId, UpdateTaskRequest request) {
        getProjectAndValidateOwnership(pmUserId, projectId);
        Task task = getTask(taskId, projectId);

        if (isTerminal(task.getStatus())) {
            throw new BusinessRuleException("Task 已為終態，無法修改");
        }

        task.setName(request.name());
        task.setBudgetHours(request.budgetHours());

        if (request.assigneeId() != null) {
            User assignee = userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.assigneeId()));
            if (!assignee.getRoles().contains(Role.EXECUTOR)) {
                throw new BusinessRuleException("指定的執行人員無 EXECUTOR 角色");
            }
            task.setAssignee(assignee);
        } else {
            task.setAssignee(null);
        }

        task = taskRepository.save(task);
        return toResponse(task);
    }

    public TaskResponse closeTask(Long pmUserId, Long projectId, Long taskId) {
        getProjectAndValidateOwnership(pmUserId, projectId);
        Task task = getTask(taskId, projectId);

        if (isTerminal(task.getStatus())) {
            throw new BusinessRuleException("Task 已為終態");
        }

        task.setStatus(TaskStatus.CLOSED);
        task = taskRepository.save(task);

        auditLogService.log(AuditActionType.TASK_FORCE_CLOSED, pmUserId, "Task", taskId,
                "PM force closed task: " + task.getName());

        return toResponse(task);
    }

    public void deleteTask(Long pmUserId, Long projectId, Long taskId) {
        getProjectAndValidateOwnership(pmUserId, projectId);
        Task task = getTask(taskId, projectId);

        if (workEntryRepository.existsByTaskId(taskId)) {
            throw new BusinessRuleException("Task 已有工時紀錄，無法刪除", HttpStatus.CONFLICT);
        }

        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getTasks(Long projectId, Pageable pageable) {
        Page<Task> page = taskRepository.findByProjectId(projectId, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    private Project getProjectAndValidateOwnership(Long pmUserId, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
        if (!project.getPm().getId().equals(pmUserId)) {
            throw new BusinessRuleException("此專案非您負責", HttpStatus.FORBIDDEN);
        }
        return project;
    }

    private Task getTask(Long taskId, Long projectId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        if (!task.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Task not found in project: " + taskId);
        }
        return task;
    }

    private boolean isTerminal(TaskStatus status) {
        return status == TaskStatus.COMPLETED || status == TaskStatus.CLOSED;
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
