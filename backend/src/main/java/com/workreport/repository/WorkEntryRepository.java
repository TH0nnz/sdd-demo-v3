package com.workreport.repository;

import com.workreport.entity.WorkEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface WorkEntryRepository extends JpaRepository<WorkEntry, Long> {

    List<WorkEntry> findByUserIdAndWorkDateBetween(Long userId, LocalDate start, LocalDate end);

    Page<WorkEntry> findByUserIdAndWorkDateBetween(Long userId, LocalDate start, LocalDate end, Pageable pageable);

    List<WorkEntry> findByTaskIdAndWorkDate(Long taskId, LocalDate workDate);

    boolean existsByTaskId(Long taskId);

    boolean existsByTaskProjectId(Long projectId);

    @Query("SELECT COALESCE(SUM(w.hours), 0) FROM WorkEntry w WHERE w.user.id = :userId AND w.workDate = :date")
    BigDecimal sumHoursByUserIdAndWorkDate(@Param("userId") Long userId, @Param("date") LocalDate date);
}
