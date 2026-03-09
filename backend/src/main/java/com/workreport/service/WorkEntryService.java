package com.workreport.service;

import com.workreport.dto.common.PageResponse;
import com.workreport.dto.workentry.CreateWorkEntryRequest;
import com.workreport.dto.workentry.UpdateWorkEntryRequest;
import com.workreport.dto.workentry.WorkEntryResponse;
import com.workreport.entity.Task;
import com.workreport.entity.User;
import com.workreport.entity.WorkEntry;
import com.workreport.enums.NotificationType;
import com.workreport.enums.TaskStatus;
import com.workreport.exception.BusinessRuleException;
import com.workreport.exception.ResourceNotFoundException;
import com.workreport.repository.TaskRepository;
import com.workreport.repository.UserRepository;
import com.workreport.repository.WorkEntryRepository;
import com.workreport.util.WorkDayUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional
public class WorkEntryService {

    private static final BigDecimal HALF = new BigDecimal("0.5");
    private static final BigDecimal DAILY_LIMIT = new BigDecimal("24.0");

    private final WorkEntryRepository workEntryRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public WorkEntryService(WorkEntryRepository workEntryRepository,
                            TaskRepository taskRepository,
                            UserRepository userRepository,
                            NotificationService notificationService) {
        this.workEntryRepository = workEntryRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public WorkEntryResponse createWorkEntry(Long userId, CreateWorkEntryRequest request) {
        Task task = taskRepository.findById(request.taskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + request.taskId()));

        // Validate not terminal status
        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.CLOSED) {
            throw new BusinessRuleException("Cannot log hours to a " + task.getStatus() + " task");
        }

        // Validate task is assigned to this user
        if (task.getAssignee() == null || !task.getAssignee().getId().equals(userId)) {
            throw new BusinessRuleException("Task is not assigned to you", HttpStatus.FORBIDDEN);
        }

        // Validate workDate is within editable range
        if (!WorkDayUtils.isEditable(request.workDate(), LocalDate.now())) {
            throw new BusinessRuleException("Work date is outside the editable range");
        }

        // Validate hours is multiple of 0.5
        if (request.hours().remainder(HALF).compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessRuleException("Hours must be a multiple of 0.5");
        }

        // Validate daily total hours <= 24
        BigDecimal dailyTotal = workEntryRepository.sumHoursByUserIdAndWorkDate(userId, request.workDate());
        if (dailyTotal.add(request.hours()).compareTo(DAILY_LIMIT) > 0) {
            throw new BusinessRuleException("Daily total hours cannot exceed 24");
        }

        // Validate task has remaining budget
        BigDecimal remaining = task.getBudgetHours().subtract(task.getConsumedHours());
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Task budget is exhausted");
        }
        // Validate hours do not exceed remaining (avoid negative remaining)
        if (request.hours().compareTo(remaining) > 0) {
            throw new BusinessRuleException("工時只剩餘 " + remaining.stripTrailingZeros().toPlainString() + " 小時");
        }

        // Create WorkEntry
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        WorkEntry entry = new WorkEntry();
        entry.setUser(user);
        entry.setTask(task);
        entry.setWorkDate(request.workDate());
        entry.setHours(request.hours());
        entry.setCreatedAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());
        workEntryRepository.save(entry);

        // Update task consumed hours
        task.setConsumedHours(task.getConsumedHours().add(request.hours()));

        // Auto-transition PENDING → IN_PROGRESS
        if (task.getStatus() == TaskStatus.PENDING) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        }

        taskRepository.save(task);

        // Notify PM if budget exhausted
        BigDecimal newRemaining = task.getBudgetHours().subtract(task.getConsumedHours());
        if (newRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            Long pmId = task.getProject().getPm().getId();
            notificationService.notify(pmId, NotificationType.TASK_HOURS_EXHAUSTED,
                    "Task budget exhausted",
                    "Task '" + task.getName() + "' in project '" + task.getProject().getName()
                            + "' has exhausted its budget hours.");
        }

        return toResponse(entry, LocalDate.now());
    }

    public WorkEntryResponse updateWorkEntry(Long userId, Long entryId, UpdateWorkEntryRequest request) {
        WorkEntry entry = workEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkEntry not found: " + entryId));

        // Validate owned by user
        if (!entry.getUser().getId().equals(userId)) {
            throw new BusinessRuleException("You can only edit your own work entries", HttpStatus.FORBIDDEN);
        }

        Task task = entry.getTask();

        // Validate task not terminal
        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.CLOSED) {
            throw new BusinessRuleException("此任務已完成，無法修改工時");
        }

        // Validate workDate still in editable range
        if (!WorkDayUtils.isEditable(entry.getWorkDate(), LocalDate.now())) {
            throw new BusinessRuleException("Work date is outside the editable range");
        }

        // Validate new hours is multiple of 0.5
        if (request.hours().remainder(HALF).compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessRuleException("Hours must be a multiple of 0.5");
        }

        // Calculate difference, update daily total check
        BigDecimal diff = request.hours().subtract(entry.getHours());
        BigDecimal dailyTotal = workEntryRepository.sumHoursByUserIdAndWorkDate(userId, entry.getWorkDate());
        if (dailyTotal.add(diff).compareTo(DAILY_LIMIT) > 0) {
            throw new BusinessRuleException("當日總工時不得超過 24 小時");
        }

        // Validate update would not cause negative remaining
        BigDecimal remaining = task.getBudgetHours().subtract(task.getConsumedHours());
        if (diff.compareTo(BigDecimal.ZERO) > 0 && diff.compareTo(remaining) > 0) {
            throw new BusinessRuleException("工時只剩餘 " + remaining.stripTrailingZeros().toPlainString() + " 小時");
        }

        // Update entry hours and task consumed hours
        entry.setHours(request.hours());
        entry.setUpdatedAt(LocalDateTime.now());
        workEntryRepository.save(entry);

        task.setConsumedHours(task.getConsumedHours().add(diff));
        taskRepository.save(task);

        return toResponse(entry, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkEntryResponse> getWorkEntries(Long userId, LocalDate startDate,
                                                          LocalDate endDate, Pageable pageable) {
        Page<WorkEntry> page = workEntryRepository.findByUserIdAndWorkDateBetween(
                userId, startDate, endDate, pageable);
        LocalDate today = LocalDate.now();
        return PageResponse.from(page.map(entry -> toResponse(entry, today)));
    }

    private WorkEntryResponse toResponse(WorkEntry entry, LocalDate today) {
        Task task = entry.getTask();
        BigDecimal remaining = task.getBudgetHours().subtract(task.getConsumedHours());
        String warning = null;
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            warning = "Task budget exhausted";
        }
        boolean dateEditable = WorkDayUtils.isEditable(entry.getWorkDate(), today);
        boolean taskEditable = task.getStatus() != TaskStatus.COMPLETED && task.getStatus() != TaskStatus.CLOSED;
        return new WorkEntryResponse(
                entry.getId(),
                task.getId(),
                task.getName(),
                task.getProject().getName(),
                entry.getWorkDate(),
                entry.getHours(),
                dateEditable && taskEditable,
                remaining,
                warning,
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}
