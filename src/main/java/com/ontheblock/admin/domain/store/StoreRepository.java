package com.ontheblock.admin.domain.store;

import com.ontheblock.admin.domain.store.entity.StoreEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface StoreRepository extends JpaRepository<StoreEntity, UUID> {

    @Query("""
        SELECT s FROM StoreEntity s
        WHERE s.deletedAt IS NULL
          AND (:type IS NULL OR s.type = :type)
          AND (:status IS NULL OR s.status = :status)
          AND (:nameSearch IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :nameSearch, '%')))
        """)
    Page<StoreEntity> findAllWithFilters(
            @Param("type") StoreEntity.StoreType type,
            @Param("status") StoreEntity.StoreStatus status,
            @Param("nameSearch") String nameSearch,
            Pageable pageable);

    @Query("""
        SELECT s FROM StoreEntity s
        JOIN StoreManagerMappingEntity m ON m.placeId = s.id
        WHERE s.deletedAt IS NULL AND m.userId = :userId
        """)
    Page<StoreEntity> findAllByManagerUserId(@Param("userId") java.util.UUID userId, Pageable pageable);
}
