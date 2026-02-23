package com.workreport.unit.service;

import com.workreport.dto.project.CreateProjectRequest;
import com.workreport.dto.project.ProjectResponse;
import com.workreport.dto.project.UpdateProjectRequest;
import com.workreport.entity.Project;
import com.workreport.entity.User;
import com.workreport.enums.AuditActionType;
import com.workreport.enums.ProjectStatus;
import com.workreport.enums.Role;
import com.workreport.exception.BusinessRuleException;
import com.workreport.repository.ProjectRepository;
import com.workreport.repository.TaskRepository;
import com.workreport.repository.UserRepository;
import com.workreport.service.AuditLogService;
import com.workreport.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ProjectService projectService;

    private User pmUser;
    private User nonPmUser;
    private Project project;

    @BeforeEach
    void setUp() {
        pmUser = new User();
        pmUser.setId(1L);
        pmUser.setName("PM User");
        pmUser.setRoles(Set.of(Role.PM));

        nonPmUser = new User();
        nonPmUser.setId(2L);
        nonPmUser.setName("Executor User");
        nonPmUser.setRoles(Set.of(Role.EXECUTOR));

        project = new Project();
        project.setId(10L);
        project.setName("Test Project");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setTotalBudgetHours(new BigDecimal("500.0"));
        project.setConsumedHours(BigDecimal.ZERO);
        project.setPm(pmUser);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void createProject_success() {
        CreateProjectRequest request = new CreateProjectRequest("New Project", new BigDecimal("100.0"), 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(pmUser));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(20L);
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            return p;
        });

        ProjectResponse response = projectService.createProject(request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("New Project");
        assertThat(response.status()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(response.totalBudgetHours()).isEqualByComparingTo(new BigDecimal("100.0"));
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void updateProject_success() {
        UpdateProjectRequest request = new UpdateProjectRequest("Updated Name", new BigDecimal("800.0"), 1L);

        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(pmUser));
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        ProjectResponse response = projectService.updateProject(10L, request);

        assertThat(response).isNotNull();
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void closeProject_logsAudit() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        ProjectResponse response = projectService.closeProject(10L);

        assertThat(response).isNotNull();
        verify(auditLogService).log(eq(AuditActionType.PROJECT_CLOSED), any(), eq("Project"), eq(10L), anyString());
    }

    @Test
    void deleteProject_withTasks_rejected() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(taskRepository.countByProjectId(10L)).thenReturn(3L);

        assertThatThrownBy(() -> projectService.deleteProject(10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Task");

        verify(projectRepository, never()).delete(any());
    }

    @Test
    void deleteProject_withoutTasks_success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(taskRepository.countByProjectId(10L)).thenReturn(0L);

        projectService.deleteProject(10L);

        verify(projectRepository).delete(project);
    }

    @Test
    void createProject_assignPm_success() {
        CreateProjectRequest request = new CreateProjectRequest("PM Project", new BigDecimal("200.0"), 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(pmUser));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(30L);
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            return p;
        });

        ProjectResponse response = projectService.createProject(request);

        assertThat(response.pmId()).isEqualTo(1L);
        assertThat(response.pmName()).isEqualTo("PM User");
    }

    @Test
    void createProject_assignNonPmUser_rejected() {
        CreateProjectRequest request = new CreateProjectRequest("Bad PM Project", new BigDecimal("200.0"), 2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(nonPmUser));

        assertThatThrownBy(() -> projectService.createProject(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PM");
    }
}
