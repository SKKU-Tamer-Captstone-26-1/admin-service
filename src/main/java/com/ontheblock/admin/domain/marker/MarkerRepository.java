package com.ontheblock.admin.domain.marker;

import com.ontheblock.admin.domain.marker.entity.MarkerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MarkerRepository extends JpaRepository<MarkerEntity, UUID> {

    @Query("""
        SELECT m FROM MarkerEntity m
        WHERE m.deletedAt IS NULL
          AND (:layerCode IS NULL OR m.layerCode = :layerCode)
          AND (:visibility IS NULL OR m.visibility = :visibility)
          AND (:labelSearch IS NULL OR LOWER(m.label) LIKE LOWER(CONCAT('%', :labelSearch, '%')))
          AND (:geohashPrefix IS NULL OR m.geohash LIKE CONCAT(:geohashPrefix, '%'))
          AND (:placeRef IS NULL OR m.placeRef = :placeRef)
        """)
    Page<MarkerEntity> findAllWithFilters(
            @Param("layerCode") String layerCode,
            @Param("visibility") MarkerEntity.Visibility visibility,
            @Param("labelSearch") String labelSearch,
            @Param("geohashPrefix") String geohashPrefix,
            @Param("placeRef") UUID placeRef,
            Pageable pageable);

    boolean existsByLayerCodeAndDeletedAtIsNull(String layerCode);

    List<MarkerEntity> findAllByPlaceRefAndDeletedAtIsNull(UUID placeRef);

    @Query("SELECT COUNT(m) FROM MarkerEntity m WHERE m.layerCode = :layerCode AND m.deletedAt IS NULL")
    int countByLayerCode(@Param("layerCode") String layerCode);
}
