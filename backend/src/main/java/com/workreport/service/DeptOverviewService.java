package com.workreport.service;

import com.workreport.annotation.RequireRole;
import com.workreport.dto.department.DeptOverviewResponse;
import com.workreport.dto.department.MemberTaskResponse;
import com.workreport.dto.department.MemberSummaryDto;
import com.workreport.entity.Task;
import com.workreport.entity.User;
import com.workreport.enums.Role;
import com.workreport.enums.TaskStatus;
import com.workreport.exception.BusinessRuleException;
import com.workreport.exception.ResourceNotFoundException;
import com.workreport.repository.TaskRepository;
import com.workreport.repository.UserRepository;
import com.workreport.repository.WorkEntryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DeptOverviewService {

    private final UserRepository userRepository;
    private final WorkEntryRepository workEntryRepository;
        private final TaskRepository taskRepository;

    public DeptOverviewService(UserRepository userRepository,
                                                           WorkEntryRepository workEntryRepository,
                                                           TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.workEntryRepository = workEntryRepository;
                this.taskRepository = taskRepository;
    }

        @RequireRole({Role.DEPT_MANAGER, Role.ADMIN})
    public DeptOverviewResponse getDepartmentOverview(Long deptManagerUserId) {
        User manager = userRepository.findById(deptManagerUserId)
                .orElseThrow(() -> new BusinessRuleException("User not found", HttpStatus.NOT_FOUND));

        Long departmentId = manager.getDepartment().getId();
        String deptName = manager.getDepartment().getName();

        List<User> members = userRepository.findByDepartmentId(departmentId);

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        List<MemberSummaryDto> memberSummaries = members.stream()
                .map(member -> {
                    BigDecimal weeklyHours = sumHours(member.getId(), weekStart, today);
                    BigDecimal monthlyHours = sumHours(member.getId(), monthStart, monthEnd);
                    BigDecimal todayHours = workEntryRepository
                            .sumHoursByUserIdAndWorkDate(member.getId(), today);
                    return new MemberSummaryDto(
                            member.getId(),
                            member.getName(),
                            weeklyHours,
                            monthlyHours,
                            todayHours
                    );
                })
                .toList();

        BigDecimal totalHoursThisWeek = memberSummaries.stream()
                .map(MemberSummaryDto::totalHoursThisWeek)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalHoursThisMonth = memberSummaries.stream()
                .map(MemberSummaryDto::totalHoursThisMonth)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DeptOverviewResponse(deptName, members.size(), totalHoursThisWeek, totalHoursThisMonth, memberSummaries);
    }

        @RequireRole({Role.DEPT_MANAGER, Role.ADMIN})
        public List<MemberSummaryDto> getDepartmentMembers(Long deptManagerUserId) {
                return getDepartmentOverview(deptManagerUserId).members();
        }

        @RequireRole({Role.DEPT_MANAGER, Role.ADMIN})
    public List<MemberTaskResponse> getDepartmentMemberTasks(Long deptManagerUserId, Long memberUserId) {
        User manager = userRepository.findById(deptManagerUserId)
                .orElseThrow(() -> new BusinessRuleException("User not found", HttpStatus.NOT_FOUND));
        User member = userRepository.findById(memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + memberUserId));

        if (!manager.getDepartment().getId().equals(member.getDepartment().getId())) {
            throw new BusinessRuleException("該使用者不屬於您的部門", HttpStatus.FORBIDDEN);
        }

        List<TaskStatus> statuses = List.of(TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED);
        List<Task> tasks = taskRepository.findByAssigneeIdAndStatusIn(memberUserId, statuses);
        return tasks.stream()
                .map(task -> new MemberTaskResponse(
                        task.getId(),
                        task.getName(),
                        task.getProject().getName(),
                        task.getStatus(),
                        task.getConsumedHours()
                ))
                .toList();
    }

    private BigDecimal sumHours(Long userId, LocalDate start, LocalDate end) {
        return workEntryRepository.findByUserIdAndWorkDateBetween(userId, start, end)
                .stream()
                .map(we -> we.getHours())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
