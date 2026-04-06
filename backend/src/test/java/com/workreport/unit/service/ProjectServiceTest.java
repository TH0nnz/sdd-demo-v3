package com.workreport.unit.service;

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
import com.workreport.exception.BusinessRuleException;
import com.workreport.exception.ResourceNotFoundException;
import com.workreport.repository.DepartmentRepository;
import com.workreport.repository.ProjectRepository;
import com.workreport.repository.TaskRepository;
import com.workreport.repository.UserRepository;
import com.workreport.repository.WorkEntryRepository;
import com.workreport.service.AuditLogService;
import com.workreport.service.ProjectService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private DepartmentRepository departmentRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private WorkEntryRepository workEntryRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ProjectService projectService;

    private User pmUser;
    private User nonPmUser;
    private Department department;
    private Project project;
    private static final Long PROJECT_ID = 1L;
    private static final Long PM_ID = 10L;
    private static final Long DEPT_ID = 20L;
    private static final Long ADMIN_ID = 99L;
    private static final LocalDateTime NOW = LocalDateTime.of(2025, 3, 1, 12, 0);

    @BeforeEach
    void setUp() {
        Authentication auth = new UsernamePasswordAuthenticationToken(ADMIN_ID, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        pmUser = new User();
        pmUser.setId(PM_ID);
        pmUser.setName("PM User");
        pmUser.setRoles(Set.of(Role.PM));

        nonPmUser = new User();
        nonPmUser.setId(2L);
        nonPmUser.setName("Non PM");
        nonPmUser.setRoles(Set.of(Role.EXECUTOR));

        department = new Department();
        department.setId(DEPT_ID);
        department.setName("IT");

        project = new Project();
        project.setId(PROJECT_ID);
        project.setName("Test Project");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setTotalBudgetHours(BigDecimal.valueOf(100));
        project.setConsumedHours(BigDecimal.ZERO);
        project.setPm(pmUser);
        project.setDepartment(department);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        project.setCreatedAt(NOW);
        project.setUpdatedAt(NOW);
        project.setClosedAt(null);
    }

    @Nested
    @DisplayName("createProject")
    class CreateProject {

        @Test
        @DisplayName("成功建立專案並回傳 ProjectResponse")
        void createProject_success() {
            CreateProjectRequest request = new CreateProjectRequest(
                    "New Project",
                    BigDecimal.valueOf(80),
                    PM_ID,
                    DEPT_ID
            );
            when(userRepository.findById(PM_ID)).thenReturn(Optional.of(pmUser));
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
                Project p = inv.getArgument(0);
                p.setId(PROJECT_ID);
                p.setCreatedAt(NOW);
                p.setUpdatedAt(NOW);
                return p;
            });

            ProjectResponse response = projectService.createProject(request);

            assertThat(response).isNotNull();
            assertThat(response.name()).isEqualTo("New Project");
            assertThat(response.status()).isEqualTo(ProjectStatus.ACTIVE);
            assertThat(response.totalBudgetHours()).isEqualByComparingTo(BigDecimal.valueOf(80));
            assertThat(response.consumedHours()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.pmId()).isEqualTo(PM_ID);
            assertThat(response.pmName()).isEqualTo("PM User");
            assertThat(response.departmentId()).isEqualTo(DEPT_ID);
            assertThat(response.departmentName()).isEqualTo("IT");
            assertThat(response.id()).isEqualTo(PROJECT_ID);

            ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
            verify(projectRepository).save(captor.capture());
            Project saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("New Project");
            assertThat(saved.getPm()).isSameAs(pmUser);
            assertThat(saved.getDepartment()).isSameAs(department);
        }

        @Test
        @DisplayName("pm 不存在時拋出 ResourceNotFoundException")
        void createProject_pmNotFound() {
            CreateProjectRequest request = new CreateProjectRequest(
                    "New Project",
                    BigDecimal.valueOf(80),
                    999L,
                    DEPT_ID
            );
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.createProject(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found: 999");
            verify(departmentRepository, never()).findById(any());
            verify(projectRepository, never()).save(any());
        }

        @Test
        @DisplayName("pm 無 PM 角色時拋出 BusinessRuleException")
        void createProject_pmWithoutRole() {
            CreateProjectRequest request = new CreateProjectRequest(
                    "New Project",
                    BigDecimal.valueOf(80),
                    nonPmUser.getId(),
                    DEPT_ID
            );
            when(userRepository.findById(nonPmUser.getId())).thenReturn(Optional.of(nonPmUser));

            assertThatThrownBy(() -> projectService.createProject(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("指定的使用者無 PM 角色");
            verify(departmentRepository, never()).findById(any());
            verify(projectRepository, never()).save(any());
        }

        @Test
        @DisplayName("department 不存在時拋出 ResourceNotFoundException")
        void createProject_departmentNotFound() {
            CreateProjectRequest request = new CreateProjectRequest(
                    "New Project",
                    BigDecimal.valueOf(80),
                    PM_ID,
                    999L
            );
            when(userRepository.findById(PM_ID)).thenReturn(Optional.of(pmUser));
            when(departmentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.createProject(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Department not found: 999");
            verify(projectRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateProject")
    class UpdateProject {

        @Test
        @DisplayName("成功更新專案名稱、總時數、pm")
        void updateProject_success() {
            UpdateProjectRequest request = new UpdateProjectRequest(
                    "Updated Name",
                    BigDecimal.valueOf(120),
                    PM_ID,
                    DEPT_ID
            );
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(userRepository.findById(PM_ID)).thenReturn(Optional.of(pmUser));
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

            ProjectResponse response = projectService.updateProject(PROJECT_ID, request);

            assertThat(response).isNotNull();
            assertThat(response.name()).isEqualTo("Updated Name");
            assertThat(response.totalBudgetHours()).isEqualByComparingTo(BigDecimal.valueOf(120));
            assertThat(response.pmId()).isEqualTo(PM_ID);

            ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
            verify(projectRepository).save(captor.capture());
            Project saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("Updated Name");
            assertThat(saved.getTotalBudgetHours()).isEqualByComparingTo(BigDecimal.valueOf(120));
            assertThat(saved.getPm()).isSameAs(pmUser);
        }

        @Test
        @DisplayName("專案不存在時拋出 ResourceNotFoundException")
        void updateProject_projectNotFound() {
            UpdateProjectRequest request = new UpdateProjectRequest(
                    "Name",
                    BigDecimal.ONE,
                    PM_ID,
                    DEPT_ID
            );
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.updateProject(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Project not found: 999");
            verify(userRepository, never()).findById(any());
            verify(projectRepository, never()).save(any());
        }

        @Test
        @DisplayName("專案已關閉時拋出 BusinessRuleException")
        void updateProject_projectClosed() {
            project.setStatus(ProjectStatus.CLOSED);
            UpdateProjectRequest request = new UpdateProjectRequest(
                    "Name",
                    BigDecimal.ONE,
                    PM_ID,
                    DEPT_ID
            );
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.updateProject(PROJECT_ID, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("專案已關閉，無法修改");
            verify(userRepository, never()).findById(any());
            verify(projectRepository, never()).save(any());
        }

        @Test
        @DisplayName("指定的 pm 不存在時拋出 ResourceNotFoundException")
        void updateProject_pmNotFound() {
            UpdateProjectRequest request = new UpdateProjectRequest(
                    "Name",
                    BigDecimal.ONE,
                    999L,
                    DEPT_ID
            );
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.updateProject(PROJECT_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found: 999");
            verify(projectRepository, never()).save(any());
        }

        @Test
        @DisplayName("指定的 pm 無 PM 角色時拋出 BusinessRuleException")
        void updateProject_pmWithoutRole() {
            UpdateProjectRequest request = new UpdateProjectRequest(
                    "Name",
                    BigDecimal.ONE,
                    nonPmUser.getId(),
                    DEPT_ID
            );
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(userRepository.findById(nonPmUser.getId())).thenReturn(Optional.of(nonPmUser));

            assertThatThrownBy(() -> projectService.updateProject(PROJECT_ID, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("指定的使用者無 PM 角色");
            verify(projectRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("closeProject")
    class CloseProject {

        @Test
        @DisplayName("成功關閉專案並寫入 auditLog")
        void closeProject_success() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(taskRepository.findByProjectIdAndStatusIn(eq(PROJECT_ID), any())).thenReturn(List.of());
            when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
                Project p = inv.getArgument(0);
                if (p.getClosedAt() == null) {
                    p.setClosedAt(NOW);
                }
                return p;
            });

            ProjectResponse response = projectService.closeProject(PROJECT_ID);

            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(ProjectStatus.CLOSED);

            ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
            verify(projectRepository).save(projectCaptor.capture());
            Project saved = projectCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(ProjectStatus.CLOSED);
            assertThat(saved.getClosedAt()).isNotNull();

            verify(auditLogService).log(
                    eq(AuditActionType.PROJECT_CLOSED),
                    eq(ADMIN_ID),
                    eq("Project"),
                    eq(PROJECT_ID),
                    org.mockito.ArgumentMatchers.contains("Project closed:")
            );
        }

        @Test
        @DisplayName("專案不存在時拋出 ResourceNotFoundException")
        void closeProject_projectNotFound() {
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.closeProject(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Project not found: 999");
            verify(taskRepository, never()).findByProjectIdAndStatusIn(any(), any());
            verify(projectRepository, never()).save(any());
            verify(auditLogService, never()).log(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("專案已關閉時拋出 BusinessRuleException")
        void closeProject_alreadyClosed() {
            project.setStatus(ProjectStatus.CLOSED);
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.closeProject(PROJECT_ID))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("專案已為關閉狀態");
            verify(taskRepository, never()).findByProjectIdAndStatusIn(any(), any());
            verify(projectRepository, never()).save(any());
            verify(auditLogService, never()).log(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("尚有 PENDING/IN_PROGRESS 的 task 時拋出 BusinessRuleException CONFLICT")
        void closeProject_hasNonTerminalTasks() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(taskRepository.findByProjectIdAndStatusIn(eq(PROJECT_ID), any()))
                    .thenReturn(List.of(new com.workreport.entity.Task())); // 模擬有進行中 task

            assertThatThrownBy(() -> projectService.closeProject(PROJECT_ID))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("請先關閉所有進行中的 task 後再關閉專案")
                    .satisfies(ex -> assertThat(((BusinessRuleException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
            verify(projectRepository, never()).save(any());
            verify(auditLogService, never()).log(any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("deleteProject")
    class DeleteProject {

        @Test
        @DisplayName("成功刪除無工時紀錄的專案")
        void deleteProject_success() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(workEntryRepository.existsByTaskProjectId(PROJECT_ID)).thenReturn(false);

            projectService.deleteProject(PROJECT_ID);

            verify(projectRepository).delete(project);
        }

        @Test
        @DisplayName("專案不存在時拋出 ResourceNotFoundException")
        void deleteProject_projectNotFound() {
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.deleteProject(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Project not found: 999");
            verify(workEntryRepository, never()).existsByTaskProjectId(any());
            verify(projectRepository, never()).delete(any());
        }

        @Test
        @DisplayName("專案有工時紀錄時拋出 BusinessRuleException CONFLICT")
        void deleteProject_hasWorkEntries() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(workEntryRepository.existsByTaskProjectId(PROJECT_ID)).thenReturn(true);

            assertThatThrownBy(() -> projectService.deleteProject(PROJECT_ID))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("專案已有工時紀錄，請改為關閉")
                    .satisfies(ex -> assertThat(((BusinessRuleException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
            verify(projectRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("getProjectById")
    class GetProjectById {

        @Test
        @DisplayName("專案存在時回傳 ProjectResponse")
        void getProjectById_exists() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

            ProjectResponse response = projectService.getProjectById(PROJECT_ID);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(PROJECT_ID);
            assertThat(response.name()).isEqualTo("Test Project");
            assertThat(response.status()).isEqualTo(ProjectStatus.ACTIVE);
            assertThat(response.pmId()).isEqualTo(PM_ID);
            assertThat(response.pmName()).isEqualTo("PM User");
            assertThat(response.departmentId()).isEqualTo(DEPT_ID);
            assertThat(response.departmentName()).isEqualTo("IT");
        }

        @Test
        @DisplayName("專案不存在時拋出 ResourceNotFoundException")
        void getProjectById_notExists() {
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.getProjectById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Project not found: 999");
        }

        @Test
        @DisplayName("專案無 department 時 response 的 departmentId/Name 為 null")
        void getProjectById_noDepartment() {
            project.setDepartment(null);
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

            ProjectResponse response = projectService.getProjectById(PROJECT_ID);

            assertThat(response.departmentId()).isNull();
            assertThat(response.departmentName()).isNull();
        }
    }

    @Nested
    @DisplayName("getProjects")
    class GetProjects {

        @Test
        @DisplayName("分頁回傳至少一筆專案")
        void getProjects_returnsPage() {
            Pageable pageable = PageRequest.of(0, 10);
            when(projectRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(project), pageable, 1));

            PageResponse<ProjectResponse> page = projectService.getProjects(pageable);

            assertThat(page).isNotNull();
            assertThat(page.content()).hasSize(1);
            assertThat(page.content().get(0).id()).isEqualTo(PROJECT_ID);
            assertThat(page.content().get(0).name()).isEqualTo("Test Project");
            assertThat(page.totalElements()).isEqualTo(1);
            assertThat(page.totalPages()).isEqualTo(1);
            assertThat(page.page()).isEqualTo(0);
            assertThat(page.size()).isEqualTo(10);
        }

        @Test
        @DisplayName("無專案時回傳空列表")
        void getProjects_empty() {
            Pageable pageable = PageRequest.of(0, 10);
            when(projectRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

            PageResponse<ProjectResponse> page = projectService.getProjects(pageable);

            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isEqualTo(0);
        }
    }
}
