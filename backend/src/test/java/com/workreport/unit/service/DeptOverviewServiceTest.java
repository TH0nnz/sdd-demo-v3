package com.workreport.unit.service;

import com.workreport.dto.department.DeptOverviewResponse;
import com.workreport.entity.Department;
import com.workreport.entity.User;
import com.workreport.entity.WorkEntry;
import com.workreport.enums.Role;
import com.workreport.repository.UserRepository;
import com.workreport.repository.WorkEntryRepository;
import com.workreport.service.DeptOverviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeptOverviewServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkEntryRepository workEntryRepository;

    @InjectMocks
    private DeptOverviewService deptOverviewService;

    private Department department;
    private User manager;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(1L);
        department.setName("研發部");

        manager = new User();
        manager.setId(1L);
        manager.setName("部門主管");
        manager.setDepartment(department);
        manager.setRoles(Set.of(Role.DEPT_MANAGER));
    }

    @Test
    void getDepartmentSummary_returnsStats() {
        User member1 = createMember(2L, "成員一");
        User member2 = createMember(3L, "成員二");

        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(userRepository.findByDepartmentId(1L)).thenReturn(List.of(manager, member1, member2));

        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        WorkEntry entry1 = new WorkEntry();
        entry1.setHours(new BigDecimal("8.0"));
        WorkEntry entry2 = new WorkEntry();
        entry2.setHours(new BigDecimal("4.0"));

        // Manager entries
        when(workEntryRepository.findByUserIdAndWorkDateBetween(eq(1L), eq(monthStart), eq(monthEnd)))
                .thenReturn(List.of(entry1));
        when(workEntryRepository.sumHoursByUserIdAndWorkDate(eq(1L), eq(today)))
                .thenReturn(new BigDecimal("2.0"));

        // Member1 entries
        when(workEntryRepository.findByUserIdAndWorkDateBetween(eq(2L), eq(monthStart), eq(monthEnd)))
                .thenReturn(List.of(entry2));
        when(workEntryRepository.sumHoursByUserIdAndWorkDate(eq(2L), eq(today)))
                .thenReturn(new BigDecimal("4.0"));

        // Member2 entries
        when(workEntryRepository.findByUserIdAndWorkDateBetween(eq(3L), eq(monthStart), eq(monthEnd)))
                .thenReturn(Collections.emptyList());
        when(workEntryRepository.sumHoursByUserIdAndWorkDate(eq(3L), eq(today)))
                .thenReturn(BigDecimal.ZERO);

        DeptOverviewResponse response = deptOverviewService.getDepartmentOverview(1L);

        assertThat(response.deptName()).isEqualTo("研發部");
        assertThat(response.memberCount()).isEqualTo(3);
        assertThat(response.totalHoursThisMonth()).isEqualByComparingTo(new BigDecimal("12.0"));
        assertThat(response.members()).hasSize(3);
        assertThat(response.members().get(0).todayHours()).isEqualByComparingTo(new BigDecimal("2.0"));
    }

    @Test
    void getDepartmentSummary_noMembers_emptyStats() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(userRepository.findByDepartmentId(1L)).thenReturn(Collections.emptyList());

        DeptOverviewResponse response = deptOverviewService.getDepartmentOverview(1L);

        assertThat(response.deptName()).isEqualTo("研發部");
        assertThat(response.memberCount()).isZero();
        assertThat(response.totalHoursThisMonth()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.members()).isEmpty();
    }

    private User createMember(Long id, String name) {
        User member = new User();
        member.setId(id);
        member.setName(name);
        member.setDepartment(department);
        member.setRoles(Set.of(Role.EXECUTOR));
        member.setCreatedAt(LocalDateTime.now());
        return member;
    }
}
