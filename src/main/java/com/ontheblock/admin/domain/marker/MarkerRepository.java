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

    @Query(value = """
        SELECT * FROM map_view.markers m
        WHERE m.deleted_at IS NULL
          AND (CAST(:layerCode AS varchar) IS NULL OR m.layer_code = :layerCode)
          AND (CAST(:visibility AS varchar) IS NULL OR m.visibility = :visibility)
          AND (CAST(:labelSearch AS varchar) IS NULL OR LOWER(m.label) LIKE LOWER(CONCAT('%', :labelSearch, '%')))
          AND (CAST(:geohashPrefix AS varchar) IS NULL OR m.geohash LIKE CONCAT(:geohashPrefix, '%'))
          AND (CAST(:placeRef AS uuid) IS NULL OR m.place_ref = CAST(:placeRef AS uuid))
        """,
        countQuery = """
        SELECT COUNT(*) FROM map_view.markers m
        WHERE m.deleted_at IS NULL
          AND (CAST(:layerCode AS varchar) IS NULL OR m.layer_code = :layerCode)
          AND (CAST(:visibility AS varchar) IS NULL OR m.visibility = :visibility)
          AND (CAST(:labelSearch AS varchar) IS NULL OR LOWER(m.label) LIKE LOWER(CONCAT('%', :labelSearch, '%')))
          AND (CAST(:geohashPrefix AS varchar) IS NULL OR m.geohash LIKE CONCAT(:geohashPrefix, '%'))
          AND (CAST(:placeRef AS uuid) IS NULL OR m.place_ref = CAST(:placeRef AS uuid))
        """,
        nativeQuery = true)
    Page<MarkerEntity> findAllWithFilters(
            @Param("layerCode") String layerCode,
            @Param("visibility") String visibility,
            @Param("labelSearch") String labelSearch,
            @Param("geohashPrefix") String geohashPrefix,
            @Param("placeRef") UUID placeRef,
            Pageable pageable);

    boolean existsByLayerCodeAndDeletedAtIsNull(String layerCode);

    List<MarkerEntity> findAllByPlaceRefAndDeletedAtIsNull(UUID placeRef);

    @Query("SELECT COUNT(m) FROM MarkerEntity m WHERE m.layerCode = :layerCode AND m.deletedAt IS NULL")
    int countByLayerCode(@Param("layerCode") String layerCode);
}
