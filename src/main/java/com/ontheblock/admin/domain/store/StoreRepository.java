package com.ontheblock.admin.domain.store;

import com.ontheblock.admin.domain.store.entity.StoreEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface StoreRepository extends JpaRepository<StoreEntity, UUID> {

    @Query(value = """
        SELECT * FROM admin.places s
        WHERE s.deleted_at IS NULL
          AND (CAST(:type AS varchar) IS NULL OR s.type = :type)
          AND (CAST(:status AS varchar) IS NULL OR s.status = :status)
          AND (CAST(:nameSearch AS varchar) IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :nameSearch, '%')))
        """,
        countQuery = """
        SELECT COUNT(*) FROM admin.places s
        WHERE s.deleted_at IS NULL
          AND (CAST(:type AS varchar) IS NULL OR s.type = :type)
          AND (CAST(:status AS varchar) IS NULL OR s.status = :status)
          AND (CAST(:nameSearch AS varchar) IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :nameSearch, '%')))
        """,
        nativeQuery = true)
    Page<StoreEntity> findAllWithFilters(
            @Param("type") String type,
            @Param("status") String status,
            @Param("nameSearch") String nameSearch,
            Pageable pageable);

    @Query("""
        SELECT s FROM StoreEntity s
        JOIN StoreManagerMappingEntity m ON m.placeId = s.id
        WHERE s.deletedAt IS NULL AND m.userId = :userId
        """)
    Page<StoreEntity> findAllByManagerUserId(@Param("userId") java.util.UUID userId, Pageable pageable);
}
