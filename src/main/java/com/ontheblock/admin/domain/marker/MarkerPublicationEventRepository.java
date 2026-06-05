package com.ontheblock.admin.domain.marker;

import com.ontheblock.admin.domain.marker.entity.MarkerPublicationEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MarkerPublicationEventRepository extends JpaRepository<MarkerPublicationEventEntity, UUID> {

    @Query("""
        SELECT e FROM MarkerPublicationEventEntity e
        WHERE (:markerId IS NULL OR e.markerId = :markerId)
          AND (:eventType IS NULL OR e.eventType = :eventType)
          AND (:pendingOnly = FALSE OR e.consumedAt IS NULL)
        """)
    Page<MarkerPublicationEventEntity> findAllWithFilters(
            @Param("markerId") UUID markerId,
            @Param("eventType") MarkerPublicationEventEntity.EventType eventType,
            @Param("pendingOnly") boolean pendingOnly,
            Pageable pageable);

    int countByConsumedAtIsNull();

    List<MarkerPublicationEventEntity> findAllByMarkerIdIn(List<UUID> markerIds);
}
