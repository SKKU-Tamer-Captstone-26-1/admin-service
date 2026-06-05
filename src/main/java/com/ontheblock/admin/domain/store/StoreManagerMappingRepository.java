package com.ontheblock.admin.domain.store;

import com.ontheblock.admin.domain.store.entity.StoreManagerMappingEntity;
import com.ontheblock.admin.domain.store.entity.StoreManagerMappingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StoreManagerMappingRepository extends JpaRepository<StoreManagerMappingEntity, StoreManagerMappingId> {

    List<StoreManagerMappingEntity> findAllByPlaceId(UUID placeId);

    List<StoreManagerMappingEntity> findAllByUserId(UUID userId);

    void deleteByPlaceIdAndUserId(UUID placeId, UUID userId);
}
