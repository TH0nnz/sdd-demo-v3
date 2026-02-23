package com.workreport.service;

import com.workreport.dto.department.DeptOverviewResponse;
import com.workreport.dto.department.MemberSummaryDto;
import com.workreport.entity.User;
import com.workreport.exception.BusinessRuleException;
import com.workreport.repository.UserRepository;
import com.workreport.repository.WorkEntryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DeptOverviewService {

    private final UserRepository userRepository;
    private final WorkEntryRepository workEntryRepository;

    public DeptOverviewService(UserRepository userRepository,
                               WorkEntryRepository workEntryRepository) {
        this.userRepository = userRepository;
        this.workEntryRepository = workEntryRepository;
    }

    public DeptOverviewResponse getDepartmentOverview(Long deptManagerUserId) {
        User manager = userRepository.findById(deptManagerUserId)
                .orElseThrow(() -> new BusinessRuleException("User not found", HttpStatus.NOT_FOUND));

        Long departmentId = manager.getDepartment().getId();
        String deptName = manager.getDepartment().getName();

        List<User> members = userRepository.findByDepartmentId(departmentId);

        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        List<MemberSummaryDto> memberSummaries = members.stream()
                .map(member -> {
                    BigDecimal monthlyHours = sumHours(member.getId(), monthStart, monthEnd);
                    BigDecimal todayHours = workEntryRepository
                            .sumHoursByUserIdAndWorkDate(member.getId(), today);
                    return new MemberSummaryDto(
                            member.getId(),
                            member.getName(),
                            monthlyHours,
                            todayHours
                    );
                })
                .toList();

        BigDecimal totalHoursThisMonth = memberSummaries.stream()
                .map(MemberSummaryDto::totalHoursThisMonth)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DeptOverviewResponse(deptName, members.size(), totalHoursThisMonth, memberSummaries);
    }

    private BigDecimal sumHours(Long userId, LocalDate start, LocalDate end) {
        return workEntryRepository.findByUserIdAndWorkDateBetween(userId, start, end)
                .stream()
                .map(we -> we.getHours())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
