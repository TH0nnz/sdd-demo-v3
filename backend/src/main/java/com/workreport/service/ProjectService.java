package com.workreport.service;

import com.workreport.dto.common.PageResponse;
import com.workreport.dto.project.CreateProjectRequest;
import com.workreport.dto.project.ProjectResponse;
import com.workreport.dto.project.UpdateProjectRequest;
import com.workreport.entity.Project;
import com.workreport.entity.User;
import com.workreport.enums.AuditActionType;
import com.workreport.enums.ProjectStatus;
import com.workreport.enums.Role;
import com.workreport.exception.BusinessRuleException;
import com.workreport.exception.ResourceNotFoundException;
import com.workreport.repository.ProjectRepository;
import com.workreport.repository.TaskRepository;
import com.workreport.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final AuditLogService auditLogService;

    public ProjectService(ProjectRepository projectRepository,
                          UserRepository userRepository,
                          TaskRepository taskRepository,
                          AuditLogService auditLogService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.auditLogService = auditLogService;
    }

    public ProjectResponse createProject(CreateProjectRequest request) {
        User pm = userRepository.findById(request.pmId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.pmId()));
        if (!pm.getRoles().contains(Role.PM)) {
            throw new BusinessRuleException("指定的使用者無 PM 角色");
        }

        Project project = new Project();
        project.setName(request.name());
        project.setTotalBudgetHours(request.totalBudgetHours());
        project.setConsumedHours(BigDecimal.ZERO);
        project.setStatus(ProjectStatus.ACTIVE);
        project.setPm(pm);
        project = projectRepository.save(project);

        return toResponse(project);
    }

    public ProjectResponse updateProject(Long id, UpdateProjectRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));

        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new BusinessRuleException("專案已關閉，無法修改");
        }

        User pm = userRepository.findById(request.pmId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.pmId()));
        if (!pm.getRoles().contains(Role.PM)) {
            throw new BusinessRuleException("指定的使用者無 PM 角色");
        }

        project.setName(request.name());
        project.setTotalBudgetHours(request.totalBudgetHours());
        project.setPm(pm);
        project = projectRepository.save(project);

        return toResponse(project);
    }

    public ProjectResponse closeProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new BusinessRuleException("專案已為關閉狀態");
        }

        project.setStatus(ProjectStatus.CLOSED);
        project.setClosedAt(LocalDateTime.now());
        project = projectRepository.save(project);

        auditLogService.log(AuditActionType.PROJECT_CLOSED, null, "Project", projectId,
                "Project closed: " + project.getName());

        return toResponse(project);
    }

    public void deleteProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        long taskCount = taskRepository.countByProjectId(projectId);
        if (taskCount > 0) {
            throw new BusinessRuleException("專案下仍有 Task，無法刪除", HttpStatus.CONFLICT);
        }

        projectRepository.delete(project);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getProjects(Pageable pageable) {
        Page<Project> page = projectRepository.findAll(pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
        return toResponse(project);
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getStatus(),
                project.getTotalBudgetHours(),
                project.getConsumedHours(),
                project.getPm().getId(),
                project.getPm().getName(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                project.getClosedAt()
        );
    }
}
