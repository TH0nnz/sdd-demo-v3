package com.workreport.repository;

import com.workreport.entity.Task;
import com.workreport.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByProjectId(Long projectId, Pageable pageable);

    Page<Task> findByAssigneeId(Long assigneeId, Pageable pageable);

    Page<Task> findByAssigneeIdAndStatusIn(Long assigneeId, List<TaskStatus> statuses, Pageable pageable);

    List<Task> findByAssigneeIdAndStatusNot(Long assigneeId, TaskStatus status);

    List<Task> findByProjectIdAndStatusIn(Long projectId, List<TaskStatus> statuses);

    long countByProjectId(Long projectId);

    @Query("SELECT t.status, COUNT(t) FROM Task t WHERE t.project.id = :projectId GROUP BY t.status")
    List<Object[]> countByProjectIdGroupByStatus(@Param("projectId") Long projectId);
}
