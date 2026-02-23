package com.workreport.unit.service;

import com.workreport.dto.hoursrequest.CreateHoursRequestRequest;
import com.workreport.dto.hoursrequest.HoursRequestResponse;
import com.workreport.entity.HoursRequest;
import com.workreport.entity.Project;
import com.workreport.entity.Task;
import com.workreport.entity.User;
import com.workreport.enums.HoursRequestTargetType;
import com.workreport.enums.NotificationType;
import com.workreport.enums.ProjectStatus;
import com.workreport.enums.Role;
import com.workreport.exception.BusinessRuleException;
import com.workreport.repository.HoursRequestRepository;
import com.workreport.repository.ProjectRepository;
import com.workreport.repository.TaskRepository;
import com.workreport.repository.UserRepository;
import com.workreport.service.HoursRequestService;
import com.workreport.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HoursRequestServiceTest {

    @Mock
    private HoursRequestRepository hoursRequestRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private HoursRequestService hoursRequestService;

    private User pmUser;
    private User adminUser;
    private Project project;
    private Task task;

    @BeforeEach
    void setUp() {
        pmUser = new User();
        pmUser.setId(1L);
        pmUser.setName("PM");
        pmUser.setRoles(Set.of(Role.PM));

        adminUser = new User();
        adminUser.setId(10L);
        adminUser.setName("Admin");
        adminUser.setRoles(Set.of(Role.ADMIN));

        project = new Project();
        project.setId(100L);
        project.setName("Test Project");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setPm(pmUser);

        task = new Task();
        task.setId(200L);
        task.setName("Test Task");
        task.setProject(project);
    }

    @Test
    void createRequest_success() {
        CreateHoursRequestRequest request = new CreateHoursRequestRequest(
                100L, new BigDecimal("40.0"), "需要增補時數", HoursRequestTargetType.PROJECT, null);

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(pmUser));
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(adminUser));
        when(hoursRequestRepository.save(any(HoursRequest.class))).thenAnswer(inv -> {
            HoursRequest hr = inv.getArgument(0);
            hr.setId(1L);
            return hr;
        });

        HoursRequestResponse response = hoursRequestService.createRequest(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.projectId()).isEqualTo(100L);
        verify(notificationService).notify(eq(10L), eq(NotificationType.HOURS_REQUEST_SUBMITTED),
                anyString(), anyString());
    }

    @Test
    void createRequest_taskTarget_withoutTaskId_throwsException() {
        CreateHoursRequestRequest request = new CreateHoursRequestRequest(
                100L, new BigDecimal("20.0"), "需要增補", HoursRequestTargetType.TASK, null);

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> hoursRequestService.createRequest(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("targetTaskId");
    }

    @Test
    void createRequest_pmNotProjectOwner_throwsException() {
        CreateHoursRequestRequest request = new CreateHoursRequestRequest(
                100L, new BigDecimal("20.0"), "需要增補", HoursRequestTargetType.PROJECT, null);

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));

        // User ID 999 is not the PM of this project
        assertThatThrownBy(() -> hoursRequestService.createRequest(999L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("非您負責");
    }
}
