package com.ontheblock.admin.controller;

import com.ontheblock.admin.domain.marker.entity.MarkerPublicationEventEntity;
import com.ontheblock.admin.dto.MarkerDto;
import com.ontheblock.admin.dto.PageResponse;
import com.ontheblock.admin.service.MarkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Publication Events", description = "마커 발행 이벤트 모니터링 (운영자 전용)")
@RestController
@RequestMapping("/api/publication-events")
@RequiredArgsConstructor
public class PublicationEventController {

    private final MarkerService markerService;

    @Operation(summary = "발행 이벤트 목록 조회")
    @GetMapping
    public PageResponse<MarkerDto.PublicationEventResponse> listEvents(
            @RequestParam(required = false) UUID markerId,
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "false") boolean pendingOnly,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        MarkerPublicationEventEntity.EventType type = null;
        if (eventType != null && !eventType.isBlank()) {
            try {
                type = MarkerPublicationEventEntity.EventType.valueOf(eventType.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                type = null;
            }
        }
        Page<MarkerPublicationEventEntity> result = markerService.listPublicationEvents(
                markerId, type, pendingOnly, page, pageSize);
        return PageResponse.from(result, MarkerDto.PublicationEventResponse::from);
    }

    @Operation(summary = "이벤트 재처리 (consumed_at 초기화)")
    @PostMapping("/{id}/retrigger")
    public MarkerDto.PublicationEventResponse retrigger(@PathVariable UUID id) {
        return MarkerDto.PublicationEventResponse.from(markerService.retriggerPublicationEvent(id));
    }
}
