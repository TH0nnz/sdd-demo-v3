package com.workreport.unit.service;

import com.workreport.dto.workentry.CreateWorkEntryRequest;
import com.workreport.dto.workentry.UpdateWorkEntryRequest;
import com.workreport.dto.workentry.WorkEntryResponse;
import com.workreport.entity.Project;
import com.workreport.entity.Task;
import com.workreport.entity.User;
import com.workreport.entity.WorkEntry;
import com.workreport.enums.NotificationType;
import com.workreport.enums.TaskStatus;
import com.workreport.exception.BusinessRuleException;
import com.workreport.repository.TaskRepository;
import com.workreport.repository.UserRepository;
import com.workreport.repository.WorkEntryRepository;
import com.workreport.service.NotificationService;
import com.workreport.service.WorkEntryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkEntryServiceTest {

    @Mock
    private WorkEntryRepository workEntryRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private WorkEntryService workEntryService;

    private User user;
    private User pmUser;
    private Project project;
    private Task task;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();

        user = new User();
        user.setId(1L);
        user.setName("Executor");

        pmUser = new User();
        pmUser.setId(10L);
        pmUser.setName("PM");

        project = new Project();
        project.setId(1L);
        project.setName("Test Project");
        project.setPm(pmUser);

        task = new Task();
        task.setId(1L);
        task.setName("Test Task");
        task.setProject(project);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setBudgetHours(new BigDecimal("100.0"));
        task.setConsumedHours(new BigDecimal("10.0"));
        task.setAssignee(user);
        task.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createWorkEntry_success() {
        CreateWorkEntryRequest request = new CreateWorkEntryRequest(1L, today, new BigDecimal("2.0"));

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(workEntryRepository.sumHoursByUserIdAndWorkDate(1L, today)).thenReturn(BigDecimal.ZERO);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workEntryRepository.save(any(WorkEntry.class))).thenAnswer(inv -> {
            WorkEntry entry = inv.getArgument(0);
            entry.setId(100L);
            return entry;
        });
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        WorkEntryResponse response = workEntryService.createWorkEntry(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.taskId()).isEqualTo(1L);
        assertThat(response.hours()).isEqualByComparingTo(new BigDecimal("2.0"));
        assertThat(task.getConsumedHours()).isEqualByComparingTo(new BigDecimal("12.0"));
        verify(workEntryRepository).save(any(WorkEntry.class));
        verify(taskRepository).save(task);
    }

    @Test
    void createWorkEntry_taskTerminalStatus_throwsException() {
        task.setStatus(TaskStatus.COMPLETED);
        CreateWorkEntryRequest request = new CreateWorkEntryRequest(1L, today, new BigDecimal("1.0"));

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> workEntryService.createWorkEntry(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    void createWorkEntry_closedTask_throwsException() {
        task.setStatus(TaskStatus.CLOSED);
        CreateWorkEntryRequest request = new CreateWorkEntryRequest(1L, today, new BigDecimal("1.0"));

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> workEntryService.createWorkEntry(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CLOSED");
    }

    @Test
    void createWorkEntry_outsideEditableRange_throwsException() {
        // Use a date far in the past (well outside the 3 work-day window)
        LocalDate oldDate = today.minusDays(30);
        CreateWorkEntryRequest request = new CreateWorkEntryRequest(1L, oldDate, new BigDecimal("1.0"));

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> workEntryService.createWorkEntry(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("editable range");
    }

    @Test
    void createWorkEntry_hoursNotMultipleOfHalf_throwsException() {
        CreateWorkEntryRequest request = new CreateWorkEntryRequest(1L, today, new BigDecimal("1.7"));

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> workEntryService.createWorkEntry(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("multiple of 0.5");
    }

    @Test
    void createWorkEntry_dailyLimitExceeded_throwsException() {
        CreateWorkEntryRequest request = new CreateWorkEntryRequest(1L, today, new BigDecimal("2.0"));

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(workEntryRepository.sumHoursByUserIdAndWorkDate(1L, today)).thenReturn(new BigDecimal("23.0"));

        assertThatThrownBy(() -> workEntryService.createWorkEntry(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exceed 24");
    }

    @Test
    void createWorkEntry_taskBudgetExhausted_throwsException() {
        task.setConsumedHours(task.getBudgetHours()); // budget fully consumed
        CreateWorkEntryRequest request = new CreateWorkEntryRequest(1L, today, new BigDecimal("1.0"));

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> workEntryService.createWorkEntry(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exhausted");
    }

    @Test
    void createWorkEntry_triggersNotification_whenBudgetExhausted() {
        // Set remaining to exactly the hours being logged
        task.setConsumedHours(new BigDecimal("98.0")); // budget=100, remaining=2
        CreateWorkEntryRequest request = new CreateWorkEntryRequest(1L, today, new BigDecimal("2.0"));

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(workEntryRepository.sumHoursByUserIdAndWorkDate(1L, today)).thenReturn(BigDecimal.ZERO);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workEntryRepository.save(any(WorkEntry.class))).thenAnswer(inv -> {
            WorkEntry entry = inv.getArgument(0);
            entry.setId(100L);
            return entry;
        });
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        workEntryService.createWorkEntry(1L, request);

        verify(notificationService).notify(
                eq(pmUser.getId()),
                eq(NotificationType.TASK_HOURS_EXHAUSTED),
                any(String.class),
                any(String.class));
    }

    @Test
    void updateWorkEntry_success() {
        WorkEntry entry = new WorkEntry();
        entry.setId(1L);
        entry.setUser(user);
        entry.setTask(task);
        entry.setWorkDate(today);
        entry.setHours(new BigDecimal("2.0"));
        entry.setCreatedAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());

        UpdateWorkEntryRequest request = new UpdateWorkEntryRequest(new BigDecimal("3.0"));

        when(workEntryRepository.findById(1L)).thenReturn(Optional.of(entry));
        when(workEntryRepository.sumHoursByUserIdAndWorkDate(1L, today)).thenReturn(new BigDecimal("5.0"));
        when(workEntryRepository.save(any(WorkEntry.class))).thenReturn(entry);
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        WorkEntryResponse response = workEntryService.updateWorkEntry(1L, 1L, request);

        assertThat(response).isNotNull();
        assertThat(response.hours()).isEqualByComparingTo(new BigDecimal("3.0"));
        verify(workEntryRepository).save(entry);
        verify(taskRepository).save(task);
    }

    @Test
    void updateWorkEntry_notOwner_throwsException() {
        User otherUser = new User();
        otherUser.setId(999L);
        otherUser.setName("Other");

        WorkEntry entry = new WorkEntry();
        entry.setId(1L);
        entry.setUser(otherUser);
        entry.setTask(task);
        entry.setWorkDate(today);
        entry.setHours(new BigDecimal("2.0"));

        when(workEntryRepository.findById(1L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> workEntryService.updateWorkEntry(1L, 1L,
                new UpdateWorkEntryRequest(new BigDecimal("3.0"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("own work entries");
    }

    @Test
    void createWorkEntry_taskAutoTransitionToInProgress() {
        task.setStatus(TaskStatus.PENDING);
        CreateWorkEntryRequest request = new CreateWorkEntryRequest(1L, today, new BigDecimal("1.0"));

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(workEntryRepository.sumHoursByUserIdAndWorkDate(1L, today)).thenReturn(BigDecimal.ZERO);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workEntryRepository.save(any(WorkEntry.class))).thenAnswer(inv -> {
            WorkEntry entry = inv.getArgument(0);
            entry.setId(100L);
            return entry;
        });
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        workEntryService.createWorkEntry(1L, request);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }
}
