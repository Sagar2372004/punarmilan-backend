package com.punarmilan.backend.repository;

import com.punarmilan.backend.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByOrderByCreatedAtDesc(Pageable pageable);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);

    List<AuditLog> findByPerformedByEmailOrderByCreatedAtDesc(String email);

    @Query("SELECT a FROM AuditLog a WHERE a.createdAt >= :startDate AND a.createdAt <= :endDate ORDER BY a.createdAt DESC")
    List<AuditLog> findBetweenDates(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a FROM AuditLog a WHERE a.action LIKE %:action% ORDER BY a.createdAt DESC")
    List<AuditLog> findByActionContaining(@Param("action") String action);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE DATE(a.createdAt) = CURRENT_DATE")
    long countTodayLogs();

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByPerformedByEmail(String email);
}