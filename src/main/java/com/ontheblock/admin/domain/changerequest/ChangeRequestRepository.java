package com.ontheblock.admin.domain.changerequest;

import com.ontheblock.admin.domain.changerequest.entity.ChangeRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequestEntity, UUID> {

    @Query("""
        SELECT cr FROM ChangeRequestEntity cr
        WHERE (:status IS NULL OR cr.status = :status)
          AND (:requesterId IS NULL OR cr.requesterId = :requesterId)
          AND (:from IS NULL OR cr.createdAt >= :from)
          AND (:to IS NULL OR cr.createdAt <= :to)
        ORDER BY cr.createdAt DESC
        """)
    Page<ChangeRequestEntity> findAllWithFilters(
            @Param("status") ChangeRequestEntity.Status status,
            @Param("requesterId") UUID requesterId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    Page<ChangeRequestEntity> findAllByRequesterIdOrderByCreatedAtDesc(UUID requesterId, Pageable pageable);

    int countByStatus(ChangeRequestEntity.Status status);
}
