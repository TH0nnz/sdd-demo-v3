package com.workreport.service;

import com.workreport.annotation.RequireRole;
import com.workreport.dto.common.PageResponse;
import com.workreport.dto.hoursrequest.CreateHoursRequestRequest;
import com.workreport.dto.hoursrequest.HoursRequestResponse;
import com.workreport.entity.HoursRequest;
import com.workreport.entity.Project;
import com.workreport.entity.Task;
import com.workreport.entity.User;
import com.workreport.enums.HoursRequestStatus;
import com.workreport.enums.HoursRequestTargetType;
import com.workreport.enums.NotificationType;
import com.workreport.enums.Role;
import com.workreport.exception.BusinessRuleException;
import com.workreport.exception.ResourceNotFoundException;
import com.workreport.repository.HoursRequestRepository;
import com.workreport.repository.ProjectRepository;
import com.workreport.repository.TaskRepository;
import com.workreport.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class HoursRequestService {

    private final HoursRequestRepository hoursRequestRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public HoursRequestService(HoursRequestRepository hoursRequestRepository,
                               ProjectRepository projectRepository,
                               TaskRepository taskRepository,
                               UserRepository userRepository,
                               NotificationService notificationService) {
        this.hoursRequestRepository = hoursRequestRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @RequireRole({Role.PM, Role.ADMIN})
    public HoursRequestResponse createRequest(Long pmUserId, CreateHoursRequestRequest request) {
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + request.projectId()));

        if (!project.getPm().getId().equals(pmUserId)) {
            throw new BusinessRuleException("此專案非您負責", HttpStatus.FORBIDDEN);
        }

        if (request.targetType() == HoursRequestTargetType.TASK) {
            if (request.targetTaskId() == null) {
                throw new BusinessRuleException("targetType 為 TASK 時必須指定 targetTaskId");
            }
            Task task = taskRepository.findById(request.targetTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + request.targetTaskId()));
            if (!task.getProject().getId().equals(project.getId())) {
                throw new BusinessRuleException("指定的 Task 不屬於此專案");
            }
        }

        User requester = userRepository.findById(pmUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + pmUserId));

        HoursRequest hoursRequest = new HoursRequest();
        hoursRequest.setProject(project);
        hoursRequest.setRequester(requester);
        hoursRequest.setRequestedHours(request.requestedHours());
        hoursRequest.setDescription(request.description());
        hoursRequest.setTargetType(request.targetType());
        hoursRequest.setStatus(HoursRequestStatus.PENDING);
        hoursRequest.setCreatedAt(LocalDateTime.now());
        hoursRequest.setUpdatedAt(LocalDateTime.now());

        if (request.targetType() == HoursRequestTargetType.TASK && request.targetTaskId() != null) {
            Task targetTask = taskRepository.findById(request.targetTaskId()).orElseThrow();
            hoursRequest.setTargetTask(targetTask);
        }

        hoursRequest = hoursRequestRepository.save(hoursRequest);

        // Notify all admins
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (User admin : admins) {
            notificationService.notify(admin.getId(), NotificationType.HOURS_REQUEST_SUBMITTED,
                    "新時數增補申請",
                    "PM " + requester.getName() + " 提交了專案 '" + project.getName() + "' 的時數增補申請");
        }

        return toResponse(hoursRequest);
    }

    @RequireRole({Role.PM, Role.ADMIN})
    @Transactional(readOnly = true)
    public PageResponse<HoursRequestResponse> getRequests(Long pmUserId, Pageable pageable) {
        Page<HoursRequest> page = hoursRequestRepository.findByRequesterId(pmUserId, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    private HoursRequestResponse toResponse(HoursRequest hr) {
        return new HoursRequestResponse(
                hr.getId(),
                hr.getProject().getId(),
                hr.getProject().getName(),
                hr.getRequester().getId(),
                hr.getRequester().getName(),
                hr.getRequestedHours(),
                hr.getDescription(),
                hr.getTargetType(),
                hr.getTargetTask() != null ? hr.getTargetTask().getId() : null,
                hr.getTargetTask() != null ? hr.getTargetTask().getName() : null,
                hr.getStatus(),
                hr.getReviewer() != null ? hr.getReviewer().getId() : null,
                hr.getReviewer() != null ? hr.getReviewer().getName() : null,
                hr.getReviewComment(),
                hr.getReviewedAt(),
                hr.getCreatedAt()
        );
    }
}
