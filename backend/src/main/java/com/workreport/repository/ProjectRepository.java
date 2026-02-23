package com.workreport.repository;

import com.workreport.entity.Project;
import com.workreport.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByPmId(Long pmId);

    Page<Project> findByStatus(ProjectStatus status, Pageable pageable);

    Page<Project> findByPmIdAndStatus(Long pmId, ProjectStatus status, Pageable pageable);

    Page<Project> findByPmId(Long pmId, Pageable pageable);
}
