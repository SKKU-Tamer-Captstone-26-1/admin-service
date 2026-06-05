package com.ontheblock.admin.dto;

import com.ontheblock.admin.domain.changerequest.entity.ChangeRequestEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public final class ChangeRequestDto {

    private ChangeRequestDto() {}

    public record ChangeRequestResponse(
            UUID id,
            UUID requesterId,
            String targetType,
            UUID targetRef,
            String status,
            UUID reviewerId,
            LocalDateTime reviewedAt,
            String reviewComment,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static ChangeRequestResponse from(ChangeRequestEntity e) {
            return new ChangeRequestResponse(e.getId(), e.getRequesterId(),
                    e.getTargetType().name(), e.getTargetRef(),
                    e.getStatus().name(), e.getReviewerId(),
                    e.getReviewedAt(), e.getReviewComment(),
                    e.getCreatedAt(), e.getUpdatedAt());
        }
    }

    public record SubmitRequest(
            @Schema(example = "STORE_INFO", allowableValues = {"STORE_INFO","PRODUCT_LIST","PRODUCT_IMAGE"})
            String targetType,
            @Schema(example = "00000000-0000-0000-0000-000000000001")
            UUID targetRef,
            @Schema(example = "{\"name\":\"새 매장명\",\"contact\":\"02-1234-5678\"}")
            String proposedChanges,
            java.util.List<String> attachmentUrls
    ) {}

    public record ApproveRequest(
            @Schema(example = "true") boolean autoRepublishMarker
    ) {}

    public record RejectRequest(
            @Schema(example = "요청 내용이 부정확합니다.", requiredMode = Schema.RequiredMode.REQUIRED)
            String reviewComment
    ) {}
}
