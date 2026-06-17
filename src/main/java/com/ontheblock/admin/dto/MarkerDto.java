package com.ontheblock.admin.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontheblock.admin.domain.marker.entity.MarkerEntity;
import com.ontheblock.admin.domain.marker.entity.MarkerPublicationEventEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MarkerDto {

    private MarkerDto() {}

    public record MarkerSummaryResponse(
            UUID id,
            String layerCode,
            String label,
            double lat,
            double lng,
            String geohash,
            String visibility,
            UUID placeRef,
            int publishedRevision,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static MarkerSummaryResponse from(MarkerEntity e) {
            return new MarkerSummaryResponse(e.getId(), e.getLayerCode(), e.getLabel(),
                    e.getLat(), e.getLng(), e.getGeohash(),
                    e.getVisibility().name(), e.getPlaceRef(),
                    e.getPublishedRevision(), e.getCreatedAt(), e.getUpdatedAt());
        }
    }

    public record CreateMarkerRequest(
            @Schema(example = "BAR") String layerCode,
            @Schema(example = "온더블록 강남점") String label,
            @Schema(example = "37.5175") double lat,
            @Schema(example = "127.0473") double lng,
            String iconKey,
            @Schema(example = "VISIBLE", allowableValues = {"VISIBLE", "HIDDEN"}) String visibility,
            UUID placeRef,
            String filterJson
    ) {}

    public record UpdateMarkerRequest(
            String layerCode,
            String label,
            double lat,
            double lng,
            String iconKey,
            @Schema(allowableValues = {"VISIBLE", "HIDDEN"}) String visibility,
            UUID placeRef,
            String filterJson
    ) {}

    public record BatchPublishRequest(
            @Schema(example = "[\"uuid1\",\"uuid2\"]") java.util.List<UUID> ids
    ) {}

    public record MarkerDetailResponse(
            UUID id,
            String layerCode,
            String label,
            double lat,
            double lng,
            String geohash,
            String visibility,
            UUID placeRef,
            int publishedRevision,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Object filterJson
    ) {
        public static MarkerDetailResponse from(MarkerEntity e, ObjectMapper objectMapper) {
            Object parsedFilterJson;
            String raw = e.getFilterJson();
            if (raw == null || raw.isBlank()) {
                parsedFilterJson = Map.of();
            } else {
                try {
                    parsedFilterJson = objectMapper.readValue(raw, Map.class);
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException("Failed to parse marker filter_json for id=" + e.getId(), ex);
                }
            }
            return new MarkerDetailResponse(e.getId(), e.getLayerCode(), e.getLabel(),
                    e.getLat(), e.getLng(), e.getGeohash(),
                    e.getVisibility().name(), e.getPlaceRef(),
                    e.getPublishedRevision(), e.getCreatedAt(), e.getUpdatedAt(),
                    parsedFilterJson);
        }
    }

    public record InventoryItem(String nameKo, String beverageCatalogRef) {}

    public record UpdateInventoryRequest(List<InventoryItem> items) {}

    public record PublicationEventResponse(
            UUID id,
            UUID markerId,
            String eventType,
            boolean pending,
            LocalDateTime consumedAt,
            LocalDateTime createdAt,
            UUID placeRef,
            int publishedRevision
    ) {
        public static PublicationEventResponse from(MarkerPublicationEventEntity e) {
            return new PublicationEventResponse(e.getId(), e.getMarkerId(),
                    e.getEventType().name(), e.getConsumedAt() == null,
                    e.getConsumedAt(), e.getCreatedAt(),
                    e.getPlaceRef(), e.getPublishedRevision());
        }
    }
}
