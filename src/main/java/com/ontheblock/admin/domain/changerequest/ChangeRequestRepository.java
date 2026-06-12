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

    @Query(value = """
        SELECT * FROM admin.change_requests cr
        WHERE (CAST(:status AS varchar) IS NULL OR cr.status = :status)
          AND (CAST(:requesterId AS uuid) IS NULL OR cr.requester_id = CAST(:requesterId AS uuid))
          AND (CAST(:from AS timestamp) IS NULL OR cr.created_at >= CAST(:from AS timestamp))
          AND (CAST(:to AS timestamp) IS NULL OR cr.created_at <= CAST(:to AS timestamp))
        """,
        countQuery = """
        SELECT COUNT(*) FROM admin.change_requests cr
        WHERE (CAST(:status AS varchar) IS NULL OR cr.status = :status)
          AND (CAST(:requesterId AS uuid) IS NULL OR cr.requester_id = CAST(:requesterId AS uuid))
          AND (CAST(:from AS timestamp) IS NULL OR cr.created_at >= CAST(:from AS timestamp))
          AND (CAST(:to AS timestamp) IS NULL OR cr.created_at <= CAST(:to AS timestamp))
        """,
        nativeQuery = true)
    Page<ChangeRequestEntity> findAllWithFilters(
            @Param("status") String status,
            @Param("requesterId") UUID requesterId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    Page<ChangeRequestEntity> findAllByRequesterIdOrderByCreatedAtDesc(UUID requesterId, Pageable pageable);

    int countByStatus(ChangeRequestEntity.Status status);
}
