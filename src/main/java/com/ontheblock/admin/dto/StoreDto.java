package com.ontheblock.admin.dto;

import com.ontheblock.admin.domain.store.entity.ProductEntity;
import com.ontheblock.admin.domain.store.entity.StoreEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class StoreDto {

    private StoreDto() {}

    public record StoreResponse(
            UUID id,
            String type,
            String name,
            String address,
            double lat,
            double lng,
            String businessHours,
            String contact,
            String status,
            List<UUID> managerUserIds,
            LocalDateTime createdAt
    ) {
        public static StoreResponse from(StoreEntity e, List<UUID> managerIds) {
            return new StoreResponse(e.getId(), e.getType().name(), e.getName(),
                    e.getAddress(), e.getLat(), e.getLng(),
                    e.getBusinessHours(), e.getContact(), e.getStatus().name(),
                    managerIds, e.getCreatedAt());
        }
    }

    public record ProductResponse(
            UUID id,
            UUID placeId,
            String name,
            String category,
            String imageUrl,
            boolean inStock,
            Integer price,
            int displayOrder
    ) {
        public static ProductResponse from(ProductEntity e) {
            return new ProductResponse(e.getId(), e.getPlaceId(), e.getName(),
                    e.getCategory(), e.getImageUrl(), e.isInStock(),
                    e.getPrice(), e.getDisplayOrder());
        }
    }

    public record CreateStoreRequest(
            @Schema(example = "BAR", allowableValues = {"BAR","LIQUOR_SHOP"}) String type,
            @Schema(example = "온더블록 강남점") String name,
            @Schema(example = "서울 강남구 테헤란로 123") String address,
            @Schema(example = "37.5175") double lat,
            @Schema(example = "127.0473") double lng,
            @Schema(example = "{\"mon\":\"18:00-02:00\",\"tue\":\"18:00-02:00\"}") String businessHours,
            @Schema(example = "02-1234-5678") String contact
    ) {}

    public record AssignManagerRequest(
            @Schema(example = "00000000-0000-0000-0000-000000000001")
            UUID userId
    ) {}
}
