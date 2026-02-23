package com.workreport.repository;

import com.workreport.entity.HoursRequest;
import com.workreport.enums.HoursRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoursRequestRepository extends JpaRepository<HoursRequest, Long> {

    Page<HoursRequest> findByRequesterId(Long requesterId, Pageable pageable);

    Page<HoursRequest> findByStatus(HoursRequestStatus status, Pageable pageable);

    Page<HoursRequest> findByProjectId(Long projectId, Pageable pageable);
}
