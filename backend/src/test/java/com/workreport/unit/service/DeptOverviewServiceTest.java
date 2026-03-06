package com.workreport.unit.service;

import com.workreport.dto.department.DeptOverviewResponse;
import com.workreport.dto.department.MemberSummaryDto;
import com.workreport.dto.department.MemberTaskResponse;
import com.workreport.entity.Department;
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
import com.workreport.service.DeptOverviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeptOverviewService")
class DeptOverviewServiceTest {

    private static final Long MANAGER_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long DEPT_ID = 10L;
    private static final String DEPT_NAME = "Engineering";

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkEntryRepository workEntryRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private DeptOverviewService deptOverviewService;

    private User manager;
    private User member;
    private Department department;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(DEPT_ID);
        department.setName(DEPT_NAME);

        manager = new User();
        manager.setId(MANAGER_ID);
        manager.setName("Manager");
        manager.setDepartment(department);

        member = new User();
        member.setId(MEMBER_ID);
        member.setName("Member");
        member.setDepartment(department);
    }

    @Nested
    @DisplayName("getDepartmentOverview")
    class GetDepartmentOverview {

        @Test
        @DisplayName("manager 不存在時拋出 BusinessRuleException")
        void whenManagerNotFound_throwsBusinessRuleException() {
            when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.empty());

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> deptOverviewService.getDepartmentOverview(MANAGER_ID));

            assertEquals("User not found", ex.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
            verify(userRepository).findById(MANAGER_ID);
        }

        @Test
        @DisplayName("成功回傳含 memberSummaries 與總時數")
        void success_returnsOverviewWithMemberSummariesAndTotals() {
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
            YearMonth currentMonth = YearMonth.from(today);
            LocalDate monthStart = currentMonth.atDay(1);
            LocalDate monthEnd = currentMonth.atEndOfMonth();

            when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
            when(userRepository.findByDepartmentId(DEPT_ID)).thenReturn(List.of(manager, member));

            when(workEntryRepository.findByUserIdAndWorkDateBetween(eq(MANAGER_ID), eq(weekStart), eq(today)))
                    .thenReturn(List.of(workEntry(MANAGER_ID, weekStart, "2.0")));
            when(workEntryRepository.findByUserIdAndWorkDateBetween(eq(MANAGER_ID), eq(monthStart), eq(monthEnd)))
                    .thenReturn(List.of(workEntry(MANAGER_ID, monthStart, "8.0")));
            when(workEntryRepository.sumHoursByUserIdAndWorkDate(MANAGER_ID, today))
                    .thenReturn(new BigDecimal("1.0"));

            when(workEntryRepository.findByUserIdAndWorkDateBetween(eq(MEMBER_ID), eq(weekStart), eq(today)))
                    .thenReturn(List.of(workEntry(MEMBER_ID, today, "3.0")));
            when(workEntryRepository.findByUserIdAndWorkDateBetween(eq(MEMBER_ID), eq(monthStart), eq(monthEnd)))
                    .thenReturn(List.of(workEntry(MEMBER_ID, monthStart, "12.0")));
            when(workEntryRepository.sumHoursByUserIdAndWorkDate(MEMBER_ID, today))
                    .thenReturn(new BigDecimal("3.0"));

            DeptOverviewResponse result = deptOverviewService.getDepartmentOverview(MANAGER_ID);

            assertEquals(DEPT_NAME, result.deptName());
            assertEquals(2, result.memberCount());
            assertEquals(new BigDecimal("5.0"), result.totalHoursThisWeek());   // 2 + 3
            assertEquals(new BigDecimal("20.0"), result.totalHoursThisMonth()); // 8 + 12

            List<MemberSummaryDto> members = result.members();
            assertEquals(2, members.size());

            MemberSummaryDto m1 = members.get(0);
            assertEquals(MANAGER_ID, m1.userId());
            assertEquals("Manager", m1.name());
            assertEquals(new BigDecimal("2.0"), m1.totalHoursThisWeek());
            assertEquals(new BigDecimal("8.0"), m1.totalHoursThisMonth());
            assertEquals(new BigDecimal("1.0"), m1.todayHours());

            MemberSummaryDto m2 = members.get(1);
            assertEquals(MEMBER_ID, m2.userId());
            assertEquals("Member", m2.name());
            assertEquals(new BigDecimal("3.0"), m2.totalHoursThisWeek());
            assertEquals(new BigDecimal("12.0"), m2.totalHoursThisMonth());
            assertEquals(new BigDecimal("3.0"), m2.todayHours());
        }

        @Test
        @DisplayName("部門無其他成員時仍正確彙總總時數")
        void whenSingleMember_correctTotals() {
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
            YearMonth currentMonth = YearMonth.from(today);
            LocalDate monthStart = currentMonth.atDay(1);
            LocalDate monthEnd = currentMonth.atEndOfMonth();

            when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
            when(userRepository.findByDepartmentId(DEPT_ID)).thenReturn(List.of(manager));

            when(workEntryRepository.findByUserIdAndWorkDateBetween(eq(MANAGER_ID), eq(weekStart), eq(today)))
                    .thenReturn(List.of());
            when(workEntryRepository.findByUserIdAndWorkDateBetween(eq(MANAGER_ID), eq(monthStart), eq(monthEnd)))
                    .thenReturn(List.of());
            when(workEntryRepository.sumHoursByUserIdAndWorkDate(MANAGER_ID, today))
                    .thenReturn(BigDecimal.ZERO);

            DeptOverviewResponse result = deptOverviewService.getDepartmentOverview(MANAGER_ID);

            assertEquals(1, result.memberCount());
            assertEquals(BigDecimal.ZERO, result.totalHoursThisWeek());
            assertEquals(BigDecimal.ZERO, result.totalHoursThisMonth());
            assertEquals(1, result.members().size());
        }
    }

    @Nested
    @DisplayName("getDepartmentMembers")
    class GetDepartmentMembers {

        @Test
        @DisplayName("回傳 getDepartmentOverview 的 members")
        void returnsMembersFromOverview() {
            LocalDate today = LocalDate.now();

            when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
            when(userRepository.findByDepartmentId(DEPT_ID)).thenReturn(List.of(manager, member));
            when(workEntryRepository.findByUserIdAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
            when(workEntryRepository.sumHoursByUserIdAndWorkDate(any(), eq(today))).thenReturn(BigDecimal.ZERO);

            List<MemberSummaryDto> result = deptOverviewService.getDepartmentMembers(MANAGER_ID);

            assertEquals(2, result.size());
            assertEquals(MANAGER_ID, result.get(0).userId());
            assertEquals(MEMBER_ID, result.get(1).userId());
        }
    }

    @Nested
    @DisplayName("getDepartmentMemberTasks")
    class GetDepartmentMemberTasks {

        @Test
        @DisplayName("manager 不存在時拋出 BusinessRuleException")
        void whenManagerNotFound_throwsBusinessRuleException() {
            when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.empty());

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> deptOverviewService.getDepartmentMemberTasks(MANAGER_ID, MEMBER_ID));

            assertEquals("User not found", ex.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        }

        @Test
        @DisplayName("member 不存在時拋出 ResourceNotFoundException")
        void whenMemberNotFound_throwsResourceNotFoundException() {
            when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> deptOverviewService.getDepartmentMemberTasks(MANAGER_ID, MEMBER_ID));

            assertEquals("User not found: " + MEMBER_ID, ex.getMessage());
        }

        @Test
        @DisplayName("member 不同部門時拋出 BusinessRuleException FORBIDDEN")
        void whenMemberDifferentDepartment_throwsBusinessRuleExceptionForbidden() {
            Department otherDept = new Department();
            otherDept.setId(99L);
            otherDept.setName("Other");
            member.setDepartment(otherDept);

            when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> deptOverviewService.getDepartmentMemberTasks(MANAGER_ID, MEMBER_ID));

            assertEquals("該使用者不屬於您的部門", ex.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verify(taskRepository, never()).findByAssigneeIdAndStatusIn(any(), any());
        }

        @Test
        @DisplayName("成功回傳 IN_PROGRESS 與 COMPLETED 的 task 列表")
        void success_returnsMemberTaskList() {
            Project project = new Project();
            project.setId(100L);
            project.setName("Project X");

            Task task1 = new Task();
            task1.setId(201L);
            task1.setName("Task A");
            task1.setProject(project);
            task1.setStatus(TaskStatus.IN_PROGRESS);
            task1.setConsumedHours(new BigDecimal("2.5"));

            Task task2 = new Task();
            task2.setId(202L);
            task2.setName("Task B");
            task2.setProject(project);
            task2.setStatus(TaskStatus.COMPLETED);
            task2.setConsumedHours(new BigDecimal("5.0"));

            when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
            when(taskRepository.findByAssigneeIdAndStatusIn(eq(MEMBER_ID), eq(List.of(TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED))))
                    .thenReturn(List.of(task1, task2));

            List<MemberTaskResponse> result = deptOverviewService.getDepartmentMemberTasks(MANAGER_ID, MEMBER_ID);

            assertEquals(2, result.size());
            assertEquals(201L, result.get(0).taskId());
            assertEquals("Task A", result.get(0).taskName());
            assertEquals("Project X", result.get(0).projectName());
            assertEquals(TaskStatus.IN_PROGRESS, result.get(0).status());
            assertEquals(new BigDecimal("2.5"), result.get(0).consumedHours());

            assertEquals(202L, result.get(1).taskId());
            assertEquals("Task B", result.get(1).taskName());
            assertEquals(TaskStatus.COMPLETED, result.get(1).status());
            assertEquals(new BigDecimal("5.0"), result.get(1).consumedHours());
        }

        @Test
        @DisplayName("member 無 task 時回傳空列表")
        void whenNoTasks_returnsEmptyList() {
            when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
            when(taskRepository.findByAssigneeIdAndStatusIn(eq(MEMBER_ID), eq(List.of(TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED))))
                    .thenReturn(List.of());

            List<MemberTaskResponse> result = deptOverviewService.getDepartmentMemberTasks(MANAGER_ID, MEMBER_ID);

            assertEquals(0, result.size());
        }
    }

    private static WorkEntry workEntry(Long userId, LocalDate workDate, String hours) {
        WorkEntry we = new WorkEntry();
        we.setWorkDate(workDate);
        we.setHours(new BigDecimal(hours));
        return we;
    }
}
