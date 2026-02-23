package com.workreport.service;

import com.workreport.dto.common.PageResponse;
import com.workreport.dto.hoursrequest.HoursRequestResponse;
import com.workreport.dto.hoursrequest.ReviewHoursRequestRequest;
import com.workreport.entity.HoursRequest;
import com.workreport.entity.User;
import com.workreport.enums.HoursRequestStatus;
import com.workreport.enums.HoursRequestTargetType;
import com.workreport.enums.NotificationType;
import com.workreport.exception.BusinessRuleException;
import com.workreport.exception.ResourceNotFoundException;
import com.workreport.repository.HoursRequestRepository;
import com.workreport.repository.ProjectRepository;
import com.workreport.repository.TaskRepository;
import com.workreport.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class HoursRequestReviewService {

    private final HoursRequestRepository hoursRequestRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public HoursRequestReviewService(HoursRequestRepository hoursRequestRepository,
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

    public HoursRequestResponse reviewRequest(Long adminUserId, Long requestId, ReviewHoursRequestRequest request) {
        if (request.decision() != HoursRequestStatus.APPROVED && request.decision() != HoursRequestStatus.REJECTED) {
            throw new BusinessRuleException("decision 只能為 APPROVED 或 REJECTED");
        }

        HoursRequest hoursRequest = hoursRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("HoursRequest not found: " + requestId));

        if (hoursRequest.getStatus() != HoursRequestStatus.PENDING) {
            throw new BusinessRuleException("此申請已被審核，無法再次審核");
        }

        User reviewer = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + adminUserId));

        hoursRequest.setReviewer(reviewer);
        hoursRequest.setReviewedAt(LocalDateTime.now());
        hoursRequest.setReviewComment(request.reviewNote());

        if (request.decision() == HoursRequestStatus.APPROVED) {
            hoursRequest.setStatus(HoursRequestStatus.APPROVED);

            if (hoursRequest.getTargetType() == HoursRequestTargetType.PROJECT) {
                var project = hoursRequest.getProject();
                project.setTotalBudgetHours(
                        project.getTotalBudgetHours().add(hoursRequest.getRequestedHours()));
                projectRepository.save(project);
            } else if (hoursRequest.getTargetType() == HoursRequestTargetType.TASK) {
                var task = hoursRequest.getTargetTask();
                task.setBudgetHours(
                        task.getBudgetHours().add(hoursRequest.getRequestedHours()));
                taskRepository.save(task);
            }

            notificationService.notify(hoursRequest.getRequester().getId(),
                    NotificationType.HOURS_REQUEST_APPROVED,
                    "時數增補申請已核准",
                    "您的專案 '" + hoursRequest.getProject().getName() + "' 時數增補申請已核准");
        } else {
            hoursRequest.setStatus(HoursRequestStatus.REJECTED);

            notificationService.notify(hoursRequest.getRequester().getId(),
                    NotificationType.HOURS_REQUEST_REJECTED,
                    "時數增補申請已駁回",
                    "您的專案 '" + hoursRequest.getProject().getName() + "' 時數增補申請已駁回");
        }

        hoursRequest.setUpdatedAt(LocalDateTime.now());
        hoursRequest = hoursRequestRepository.save(hoursRequest);

        return toResponse(hoursRequest);
    }

    @Transactional(readOnly = true)
    public PageResponse<HoursRequestResponse> getAllRequests(Pageable pageable) {
        Page<HoursRequest> page = hoursRequestRepository.findAll(pageable);
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
