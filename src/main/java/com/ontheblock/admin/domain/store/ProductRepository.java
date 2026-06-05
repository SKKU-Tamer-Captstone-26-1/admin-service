package com.ontheblock.admin.domain.store;

import com.ontheblock.admin.domain.store.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    List<ProductEntity> findAllByPlaceIdAndDeletedAtIsNullOrderByDisplayOrderAsc(UUID placeId);
}
