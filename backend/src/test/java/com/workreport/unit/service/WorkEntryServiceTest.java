package com.workreport.unit.service;

import com.workreport.dto.common.PageResponse;
import com.workreport.dto.workentry.CreateWorkEntryRequest;
import com.workreport.dto.workentry.UpdateWorkEntryRequest;
import com.workreport.dto.workentry.WorkEntryResponse;
import com.workreport.entity.Project;
import com.workreport.entity.Task;
import com.workreport.entity.User;
import com.workreport.entity.WorkEntry;
import com.workreport.enums.TaskStatus;
import com.workreport.exception.BusinessRuleException;
import com.workreport.exception.ResourceNotFoundException;
import com.workreport.repository.TaskRepository;
import com.workreport.repository.UserRepository;
import com.workreport.repository.WorkEntryRepository;
import com.workreport.service.NotificationService;
import com.workreport.service.WorkEntryService;
import com.workreport.util.WorkDayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkEntryService")
class WorkEntryServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long TASK_ID = 10L;
    private static final Long ENTRY_ID = 100L;
    private static final Long PM_ID = 2L;

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

    private User assignee;
    private User pm;
    private Project project;
    private Task task;
    private User user;

    @BeforeEach
    void setUp() {
        assignee = new User();
        assignee.setId(USER_ID);
        assignee.setName("Assignee");
        assignee.setEmail("a@test.com");
        assignee.setPasswordHash("hash");

        pm = new User();
        pm.setId(PM_ID);
        pm.setName("PM");

        project = new Project();
        project.setId(20L);
        project.setName("Project A");
        project.setPm(pm);

        task = new Task();
        task.setId(TASK_ID);
        task.setName("Task 1");
        task.setProject(project);
        task.setStatus(TaskStatus.PENDING);
        task.setBudgetHours(new BigDecimal("10.0"));
        task.setConsumedHours(BigDecimal.ZERO);
        task.setAssignee(assignee);

        user = assignee;
    }

    @Nested
    @DisplayName("createWorkEntry")
    class CreateWorkEntry {

        private LocalDate editableWorkDate() {
            return WorkDayUtils.getEditableStartDate(LocalDate.now());
        }

        @Test
        @DisplayName("task 不存在時拋出 ResourceNotFoundException")
        void whenTaskNotFound_throwsResourceNotFoundException() {
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.empty());
            CreateWorkEntryRequest request = new CreateWorkEntryRequest(
                    TASK_ID, editableWorkDate(), new BigDecimal("1.0"));

            assertThrows(ResourceNotFoundException.class,
                    () -> workEntryService.createWorkEntry(USER_ID, request));
            verify(workEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("task 為 COMPLETED 時拋出 BusinessRuleException")
        void whenTaskCompleted_throwsBusinessRuleException() {
            task.setStatus(TaskStatus.COMPLETED);
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            CreateWorkEntryRequest request = new CreateWorkEntryRequest(
                    TASK_ID, editableWorkDate(), new BigDecimal("1.0"));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> workEntryService.createWorkEntry(USER_ID, request));
            assertTrue(ex.getMessage().contains("COMPLETED"));
            verify(workEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("task 為 CLOSED 時拋出 BusinessRuleException")
        void whenTaskClosed_throwsBusinessRuleException() {
            task.setStatus(TaskStatus.CLOSED);
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            CreateWorkEntryRequest request = new CreateWorkEntryRequest(
                    TASK_ID, editableWorkDate(), new BigDecimal("1.0"));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> workEntryService.createWorkEntry(USER_ID, request));
            assertTrue(ex.getMessage().contains("CLOSED"));
            verify(workEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("非 assignee 時拋出 BusinessRuleException FORBIDDEN")
        void whenNotAssignee_throwsBusinessRuleExceptionForbidden() {
            task.setAssignee(null);
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            CreateWorkEntryRequest request = new CreateWorkEntryRequest(
                    TASK_ID, editableWorkDate(), new BigDecimal("1.0"));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> workEntryService.createWorkEntry(USER_ID, request));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("not assigned"));
        }

        @Test
        @DisplayName("assignee 為他人時拋出 BusinessRuleException FORBIDDEN")
        void whenAssigneeIsOtherUser_throwsBusinessRuleExceptionForbidden() {
            User other = new User();
            other.setId(999L);
            task.setAssignee(other);
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            CreateWorkEntryRequest request = new CreateWorkEntryRequest(
                    TASK_ID, editableWorkDate(), new BigDecimal("1.0"));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> workEntryService.createWorkEntry(USER_ID, request));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        }

        @Test
        @DisplayName("workDate 不可編輯時拋出 BusinessRuleException")
        void whenWorkDateNotEditable_throwsBusinessRuleException() {
            LocalDate notEditable = WorkDayUtils.getEditableStartDate(LocalDate.now()).minusDays(3);
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            CreateWorkEntryRequest request = new CreateWorkEntryRequest(
                    TASK_ID, notEditable, new BigDecimal("1.0"));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> workEntryService.createWorkEntry(USER_ID, request));
            assertTrue(ex.getMessage().contains("editable"));
            verify(workEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("hours 非 0.5 倍數時拋出 BusinessRuleException")
        void whenHoursNotMultipleOfHalf_throwsBusinessRuleException() {
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            CreateWorkEntryRequest request = new CreateWorkEntryRequest(
                    TASK_ID, editableWorkDate(), new BigDecimal("1.25"));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> workEntryService.createWorkEntry(USER_ID, request));
            assertTrue(ex.getMessage().contains("0.5"));
            verify(workEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("當日總時數超過 24 時拋出 BusinessRuleException")
        void whenDailyTotalExceeds24_throwsBusinessRuleException() {
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            when(workEntryRepository.sumHoursByUserIdAndWorkDate(USER_ID, editableWorkDate()))
                    .thenReturn(new BigDecimal("20.0"));
            CreateWorkEntryRequest request = new CreateWorkEntryRequest(
                    TASK_ID, editableWorkDate(), new BigDecimal("5.0"));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> workEntryService.createWorkEntry(USER_ID, request));
            assertTrue(ex.getMessage().contains("24"));
            verify(workEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("task budget 已耗盡時拋出 BusinessRuleException")
        void whenTaskBudgetExhausted_throwsBusinessRuleException() {
            task.setConsumedHours(new BigDecimal("10.0"));
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            when(workEntryRepository.sumHoursByUserIdAndWorkDate(USER_ID, editableWorkDate()))
                    .thenReturn(BigDecimal.ZERO);
            CreateWorkEntryRequest request = new CreateWorkEntryRequest(
                    TASK_ID, editableWorkDate(), new BigDecimal("1.0"));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> workEntryService.createWorkEntry(USER_ID, request));
            assertTrue(ex.getMessage().contains("budget"));
            verify(workEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("user 不存在時拋出 ResourceNotFoundException")
        void whenUserNotFound_throwsResourceNotFoundException() {
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            when(workEntryRepository.sumHoursByUserIdAndWorkDate(USER_ID, editableWorkDate()))
                    .thenReturn(BigDecimal.ZERO);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
            CreateWorkEntryRequest request = new CreateWorkEntryRequest(
                    TASK_ID, editableWorkDate(), new BigDecimal("1.0"));

            assertThrows(ResourceNotFoundException.class,
                    () -> workEntryService.createWorkEntry(USER_ID, request));
        }

        @Test
        @DisplayName("成功建立 WorkEntry 並更新 task consumedHours、PENDING 轉 IN_PROGRESS")
        void whenValid_createsEntryAndUpdatesTask() {
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            when(workEntryRepository.sumHoursByUserIdAndWorkDate(USER_ID, editableWorkDate()))
                    .thenReturn(BigDecimal.ZERO);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            WorkEntry savedEntry = new WorkEntry();
            savedEntry.setId(ENTRY_ID);
            savedEntry.setUser(user);
            savedEntry.setTask(task);
            savedEntry.setWorkDate(editableWorkDate());
            savedEntry.setHours(new BigDecimal("2.0"));
            savedEntry.setCreatedAt(LocalDateTime.now());
            savedEntry.setUpdatedAt(LocalDateTime.now());
            when(workEntryRepository.save(any(WorkEntry.class))).thenAnswer(inv -> {
                WorkEntry e = inv.getArgument(0);
                e.setId(ENTRY_ID);
                return e;
            });

            CreateWorkEntryRequest request = new CreateWorkEntryRequest(
                    TASK_ID, editableWorkDate(), new BigDecimal("2.0"));

            WorkEntryResponse response = workEntryService.createWorkEntry(USER_ID, request);

            assertNotNull(response);
            assertEquals(ENTRY_ID, response.id());
            assertEquals(TASK_ID, response.taskId());
            assertEquals(editableWorkDate(), response.workDate());
            assertEquals(new BigDecimal("2.0"), response.hours());
            assertTrue(response.editable());

            ArgumentCaptor<WorkEntry> entryCaptor = ArgumentCaptor.forClass(WorkEntry.class);
            verify(workEntryRepository).save(entryCaptor.capture());
            assertEquals(USER_ID, entryCaptor.getValue().getUser().getId());
            assertEquals(TASK_ID, entryCaptor.getValue().getTask().getId());
            assertEquals(new BigDecimal("2.0"), entryCaptor.getValue().getHours());

            verify(taskRepository).save(task);
            assertEquals(new BigDecimal("2.0"), task.getConsumedHours());
            assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());

            verify(notificationService, never()).notify(any(), any(), any(), any());
        }

        @Test
        @DisplayName("task 已為 IN_PROGRESS 時建立成功且不變更狀態")
        void whenTaskAlreadyInProgress_createsEntryWithoutChangingStatus() {
            task.setStatus(TaskStatus.IN_PROGRESS);
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            when(workEntryRepository.sumHoursByUserIdAndWorkDate(USER_ID, editableWorkDate()))
                    .thenReturn(BigDecimal.ZERO);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(workEntryRepository.save(any(WorkEntry.class))).thenAnswer(inv -> {
                WorkEntry e = inv.getArgument(0);
                e.setId(ENTRY_ID);
                return e;
            });

            CreateWorkEntryRequest request = new CreateWorkEntryRequest(
                    TASK_ID, editableWorkDate(), new BigDecimal("1.0"));

            WorkEntryResponse response = workEntryService.createWorkEntry(USER_ID, request);

            assertNotNull(response);
            assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
            verify(taskRepository).save(task);
        }

        @Test
        @DisplayName("成功建立且 budget 耗盡時通知 PM")
        void whenBudgetExhaustedAfterCreate_notifiesPm() {
            task.setConsumedHours(new BigDecimal("8.0"));
            when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            when(workEntryRepository.sumHoursByUserIdAndWorkDate(USER_ID, editableWorkDate()))
                    .thenReturn(BigDecimal.ZERO);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(workEntryRepository.save(any(WorkEntry.class))).thenAnswer(inv -> {
                WorkEntry e = inv.getArgument(0);
                e.setId(ENTRY_ID);
                return e;
            });

            CreateWorkEntryRequest request = new CreateWorkEntryRequest(
                    TASK_ID, editableWorkDate(), new BigDecimal("2.0"));

            WorkEntryResponse response = workEntryService.createWorkEntry(USER_ID, request);

            assertNotNull(response);
            verify(notificationService, times(1)).notify(
                    eq(PM_ID),
                    eq(com.workreport.enums.NotificationType.TASK_HOURS_EXHAUSTED),
                    eq("Task budget exhausted"),
                    org.mockito.ArgumentMatchers.contains("Task 1"));
        }
    }

    @Nested
    @DisplayName("updateWorkEntry")
    class UpdateWorkEntry {

        private WorkEntry entry;
        private LocalDate editableWorkDate;

        @BeforeEach
        void setUpUpdate() {
            editableWorkDate = WorkDayUtils.getEditableStartDate(LocalDate.now());
            entry = new WorkEntry();
            entry.setId(ENTRY_ID);
            entry.setUser(assignee);
            entry.setTask(task);
            entry.setWorkDate(editableWorkDate);
            entry.setHours(new BigDecimal("2.0"));
            entry.setCreatedAt(LocalDateTime.now());
            entry.setUpdatedAt(LocalDateTime.now());
        }

        @Test
        @DisplayName("entry 不存在時拋出 ResourceNotFoundException")
        void whenEntryNotFound_throwsResourceNotFoundException() {
            when(workEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.empty());
            UpdateWorkEntryRequest request = new UpdateWorkEntryRequest(new BigDecimal("3.0"));

            assertThrows(ResourceNotFoundException.class,
                    () -> workEntryService.updateWorkEntry(USER_ID, ENTRY_ID, request));
        }

        @Test
        @DisplayName("非擁有者時拋出 BusinessRuleException FORBIDDEN")
        void whenNotOwner_throwsBusinessRuleExceptionForbidden() {
            User other = new User();
            other.setId(999L);
            entry.setUser(other);
            when(workEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));

            UpdateWorkEntryRequest request = new UpdateWorkEntryRequest(new BigDecimal("3.0"));
            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> workEntryService.updateWorkEntry(USER_ID, ENTRY_ID, request));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("own"));
        }

        @Test
        @DisplayName("task 為 COMPLETED 時拋出 BusinessRuleException")
        void whenTaskCompleted_throwsBusinessRuleException() {
            task.setStatus(TaskStatus.COMPLETED);
            when(workEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));

            UpdateWorkEntryRequest request = new UpdateWorkEntryRequest(new BigDecimal("3.0"));
            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> workEntryService.updateWorkEntry(USER_ID, ENTRY_ID, request));
            assertTrue(ex.getMessage().contains("COMPLETED"));
        }

        @Test
        @DisplayName("task 為 CLOSED 時拋出 BusinessRuleException")
        void whenTaskClosed_throwsBusinessRuleException() {
            task.setStatus(TaskStatus.CLOSED);
            when(workEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));

            UpdateWorkEntryRequest request = new UpdateWorkEntryRequest(new BigDecimal("3.0"));
            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> workEntryService.updateWorkEntry(USER_ID, ENTRY_ID, request));
            assertTrue(ex.getMessage().contains("CLOSED"));
        }

        @Test
        @DisplayName("workDate 不可編輯時拋出 BusinessRuleException")
        void whenWorkDateNotEditable_throwsBusinessRuleException() {
            LocalDate notEditable = WorkDayUtils.getEditableStartDate(LocalDate.now()).minusDays(3);
            entry.setWorkDate(notEditable);
            when(workEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));

            UpdateWorkEntryRequest request = new UpdateWorkEntryRequest(new BigDecimal("3.0"));
            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> workEntryService.updateWorkEntry(USER_ID, ENTRY_ID, request));
            assertTrue(ex.getMessage().contains("editable"));
        }

        @Test
        @DisplayName("hours 非 0.5 倍數時拋出 BusinessRuleException")
        void whenHoursNotMultipleOfHalf_throwsBusinessRuleException() {
            when(workEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));

            UpdateWorkEntryRequest request = new UpdateWorkEntryRequest(new BigDecimal("3.25"));
            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> workEntryService.updateWorkEntry(USER_ID, ENTRY_ID, request));
            assertTrue(ex.getMessage().contains("0.5"));
        }

        @Test
        @DisplayName("更新後當日總時數超過 24 時拋出 BusinessRuleException")
        void whenDailyTotalExceeds24AfterUpdate_throwsBusinessRuleException() {
            when(workEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));
            when(workEntryRepository.sumHoursByUserIdAndWorkDate(USER_ID, editableWorkDate))
                    .thenReturn(new BigDecimal("22.0"));

            UpdateWorkEntryRequest request = new UpdateWorkEntryRequest(new BigDecimal("5.0"));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> workEntryService.updateWorkEntry(USER_ID, ENTRY_ID, request));
            assertTrue(ex.getMessage().contains("24"));
        }

        @Test
        @DisplayName("成功更新 entry 與 task consumedHours")
        void whenValid_updatesEntryAndTask() {
            when(workEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));
            when(workEntryRepository.sumHoursByUserIdAndWorkDate(USER_ID, editableWorkDate))
                    .thenReturn(new BigDecimal("2.0"));
            when(workEntryRepository.save(any(WorkEntry.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateWorkEntryRequest request = new UpdateWorkEntryRequest(new BigDecimal("3.0"));

            WorkEntryResponse response = workEntryService.updateWorkEntry(USER_ID, ENTRY_ID, request);

            assertNotNull(response);
            assertEquals(ENTRY_ID, response.id());
            assertEquals(new BigDecimal("3.0"), response.hours());

            verify(workEntryRepository).save(entry);
            assertEquals(new BigDecimal("3.0"), entry.getHours());

            verify(taskRepository).save(task);
            assertEquals(new BigDecimal("1.0"), task.getConsumedHours());
        }
    }

    @Nested
    @DisplayName("getWorkEntries")
    class GetWorkEntries {

        @Test
        @DisplayName("分頁查詢回傳至少一筆且含 isEditable 與 remaining")
        void returnsPageWithEditableAndRemaining() {
            WorkEntry entry = new WorkEntry();
            entry.setId(ENTRY_ID);
            entry.setUser(assignee);
            entry.setTask(task);
            task.setConsumedHours(new BigDecimal("3.0"));
            task.setBudgetHours(new BigDecimal("10.0"));
            LocalDate workDate = WorkDayUtils.getEditableStartDate(LocalDate.now());
            entry.setWorkDate(workDate);
            entry.setHours(new BigDecimal("2.0"));
            entry.setCreatedAt(LocalDateTime.now());
            entry.setUpdatedAt(LocalDateTime.now());

            Pageable pageable = PageRequest.of(0, 10);
            when(workEntryRepository.findByUserIdAndWorkDateBetween(
                    USER_ID, workDate, workDate.plusDays(1), pageable))
                    .thenReturn(new PageImpl<>(List.of(entry), pageable, 1));

            PageResponse<WorkEntryResponse> result = workEntryService.getWorkEntries(
                    USER_ID, workDate, workDate.plusDays(1), pageable);

            assertNotNull(result);
            assertEquals(1, result.content().size());
            assertEquals(1, result.totalElements());

            WorkEntryResponse first = result.content().get(0);
            assertEquals(ENTRY_ID, first.id());
            assertEquals(TASK_ID, first.taskId());
            assertEquals(new BigDecimal("2.0"), first.hours());
            assertTrue(first.editable());
            assertEquals(new BigDecimal("7.0"), first.taskRemainingHours());
            assertNull(first.warning());
        }

        @Test
        @DisplayName("task budget 耗盡時 response 含 warning")
        void whenTaskBudgetExhausted_responseContainsWarning() {
            Task exhaustedTask = new Task();
            exhaustedTask.setId(200L);
            exhaustedTask.setName("Exhausted Task");
            exhaustedTask.setProject(project);
            exhaustedTask.setStatus(TaskStatus.IN_PROGRESS);
            exhaustedTask.setBudgetHours(new BigDecimal("10.0"));
            exhaustedTask.setConsumedHours(new BigDecimal("10.0"));
            exhaustedTask.setAssignee(assignee);

            WorkEntry entry = new WorkEntry();
            entry.setId(ENTRY_ID);
            entry.setUser(assignee);
            entry.setTask(exhaustedTask);
            LocalDate workDate = WorkDayUtils.getEditableStartDate(LocalDate.now());
            entry.setWorkDate(workDate);
            entry.setHours(BigDecimal.ONE);
            entry.setCreatedAt(LocalDateTime.now());
            entry.setUpdatedAt(LocalDateTime.now());

            Pageable pageable = PageRequest.of(0, 10);
            when(workEntryRepository.findByUserIdAndWorkDateBetween(
                    USER_ID, workDate, workDate.plusDays(1), pageable))
                    .thenReturn(new PageImpl<>(List.of(entry), pageable, 1));

            PageResponse<WorkEntryResponse> result = workEntryService.getWorkEntries(
                    USER_ID, workDate, workDate.plusDays(1), pageable);

            WorkEntryResponse first = result.content().get(0);
            assertEquals(0, first.taskRemainingHours().compareTo(BigDecimal.ZERO));
            assertNotNull(first.warning());
            assertTrue(first.warning().contains("exhausted"));
        }

        @Test
        @DisplayName("空結果回傳空列表")
        void whenNoEntries_returnsEmptyContent() {
            LocalDate start = LocalDate.now().minusDays(7);
            LocalDate end = LocalDate.now();
            Pageable pageable = PageRequest.of(0, 10);
            when(workEntryRepository.findByUserIdAndWorkDateBetween(USER_ID, start, end, pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            PageResponse<WorkEntryResponse> result = workEntryService.getWorkEntries(
                    USER_ID, start, end, pageable);

            assertTrue(result.content().isEmpty());
            assertEquals(0, result.totalElements());
        }
    }
}
