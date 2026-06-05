package com.ontheblock.admin.dto;

import com.ontheblock.admin.domain.marker.entity.MarkerLayerEntity;
import io.swagger.v3.oas.annotations.media.Schema;

public final class LayerDto {

    private LayerDto() {}

    public record LayerResponse(
            String code,
            String labelKo,
            String labelEn,
            String iconKey,
            int displayOrder,
            boolean defaultVisible,
            boolean isActive,
            int markerCount
    ) {
        public static LayerResponse from(MarkerLayerEntity e, int markerCount) {
            return new LayerResponse(e.getCode(), e.getLabelKo(), e.getLabelEn(),
                    e.getIconKey(), e.getDisplayOrder(), e.isDefaultVisible(),
                    e.isActive(), markerCount);
        }
    }

    public record CreateLayerRequest(
            @Schema(example = "BAR") String code,
            @Schema(example = "바") String labelKo,
            @Schema(example = "Bar") String labelEn,
            String iconKey,
            @Schema(example = "0") int displayOrder,
            @Schema(example = "true") boolean defaultVisible
    ) {}

    public record UpdateLayerRequest(
            String labelKo,
            String labelEn,
            String iconKey,
            boolean defaultVisible,
            boolean isActive
    ) {}

    public record LayerOrderEntry(
            String code,
            int displayOrder
    ) {}

    public record ReorderRequest(java.util.List<LayerOrderEntry> entries) {}
}
