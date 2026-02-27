package com.workreport.service;

import com.workreport.dto.common.PageResponse;
import com.workreport.dto.project.CreateProjectRequest;
import com.workreport.dto.project.ProjectResponse;
import com.workreport.dto.project.UpdateProjectRequest;
import com.workreport.entity.Department;
import com.workreport.entity.Project;
import com.workreport.entity.User;
import com.workreport.enums.AuditActionType;
import com.workreport.enums.ProjectStatus;
import com.workreport.enums.Role;
import com.workreport.enums.TaskStatus;
import com.workreport.exception.BusinessRuleException;
import com.workreport.exception.ResourceNotFoundException;
import com.workreport.repository.ProjectRepository;
import com.workreport.repository.DepartmentRepository;
import com.workreport.repository.TaskRepository;
import com.workreport.repository.UserRepository;
import com.workreport.repository.WorkEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TaskRepository taskRepository;
    private final WorkEntryRepository workEntryRepository;
    private final AuditLogService auditLogService;

    public ProjectService(ProjectRepository projectRepository,
                          UserRepository userRepository,
                          DepartmentRepository departmentRepository,
                          TaskRepository taskRepository,
                          WorkEntryRepository workEntryRepository,
                          AuditLogService auditLogService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.taskRepository = taskRepository;
        this.workEntryRepository = workEntryRepository;
        this.auditLogService = auditLogService;
    }

    public ProjectResponse createProject(CreateProjectRequest request) {
        User pm = userRepository.findById(request.pmId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.pmId()));
        if (!pm.getRoles().contains(Role.PM)) {
            throw new BusinessRuleException("指定的使用者無 PM 角色");
        }

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + request.departmentId()));

        Project project = new Project();
        project.setName(request.name());
        project.setTotalBudgetHours(request.totalBudgetHours());
        project.setConsumedHours(BigDecimal.ZERO);
        project.setStatus(ProjectStatus.ACTIVE);
        project.setPm(pm);
        project.setDepartment(department);
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

        List<TaskStatus> nonTerminal = List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS);
        boolean hasNonTerminalTasks = !taskRepository
                .findByProjectIdAndStatusIn(projectId, nonTerminal)
                .isEmpty();
        if (hasNonTerminalTasks) {
            throw new BusinessRuleException("請先關閉所有進行中的 task 後再關閉專案", HttpStatus.CONFLICT);
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

        if (workEntryRepository.existsByTaskProjectId(projectId)) {
            throw new BusinessRuleException("專案已有工時紀錄，請改為關閉", HttpStatus.CONFLICT);
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
        Department department = project.getDepartment();
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getStatus(),
                project.getTotalBudgetHours(),
                project.getConsumedHours(),
                project.getPm().getId(),
                project.getPm().getName(),
                department != null ? department.getId() : null,
                department != null ? department.getName() : null,
                project.getCreatedAt(),
                project.getUpdatedAt(),
                project.getClosedAt()
        );
    }
}
