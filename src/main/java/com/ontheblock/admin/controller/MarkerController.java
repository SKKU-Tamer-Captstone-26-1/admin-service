package com.ontheblock.admin.controller;

import com.ontheblock.admin.domain.marker.entity.MarkerEntity;
import com.ontheblock.admin.dto.MarkerDto;
import com.ontheblock.admin.dto.PageResponse;
import com.ontheblock.admin.service.MarkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Markers", description = "마커 관리 (운영자 전용)")
@RestController
@RequestMapping("/api/markers")
@RequiredArgsConstructor
public class MarkerController {

    private final MarkerService markerService;

    @Operation(summary = "마커 목록 조회")
    @GetMapping
    public PageResponse<MarkerDto.MarkerSummaryResponse> listMarkers(
            @RequestParam(required = false) String layerCode,
            @RequestParam(required = false) String labelSearch,
            @RequestParam(required = false) String geohashPrefix,
            @RequestParam(required = false) UUID placeRef,
            @RequestParam(required = false) String visibility,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        MarkerEntity.Visibility vis = null;
        if (visibility != null && !visibility.isBlank()) {
            vis = MarkerEntity.Visibility.valueOf(visibility);
        }
        Page<MarkerEntity> result = markerService.listMarkers(
                layerCode, vis, labelSearch, geohashPrefix, placeRef, page, pageSize);
        return PageResponse.from(result, MarkerDto.MarkerSummaryResponse::from);
    }

    @Operation(summary = "마커 상세 조회")
    @GetMapping("/{id}")
    public MarkerDto.MarkerSummaryResponse getMarker(@PathVariable UUID id) {
        return MarkerDto.MarkerSummaryResponse.from(markerService.getMarker(id));
    }

    @Operation(summary = "마커 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MarkerDto.MarkerSummaryResponse createMarker(@RequestBody MarkerDto.CreateMarkerRequest req) {
        MarkerEntity.Visibility vis = req.visibility() != null
                ? MarkerEntity.Visibility.valueOf(req.visibility())
                : MarkerEntity.Visibility.VISIBLE;
        return MarkerDto.MarkerSummaryResponse.from(markerService.createMarker(
                req.layerCode(), req.label(), req.lat(), req.lng(),
                req.iconKey(), vis, req.placeRef(), req.filterJson()));
    }

    @Operation(summary = "마커 수정")
    @PatchMapping("/{id}")
    public MarkerDto.MarkerSummaryResponse updateMarker(
            @PathVariable UUID id, @RequestBody MarkerDto.UpdateMarkerRequest req) {
        MarkerEntity.Visibility vis = req.visibility() != null
                ? MarkerEntity.Visibility.valueOf(req.visibility())
                : MarkerEntity.Visibility.VISIBLE;
        return MarkerDto.MarkerSummaryResponse.from(markerService.updateMarker(
                id, req.layerCode(), req.label(), req.lat(), req.lng(),
                req.iconKey(), vis, req.placeRef(), req.filterJson()));
    }

    @Operation(summary = "마커 발행")
    @PostMapping("/{id}/publish")
    public MarkerDto.MarkerSummaryResponse publishMarker(
            @PathVariable UUID id, HttpServletRequest request) {
        return MarkerDto.MarkerSummaryResponse.from(
                markerService.publishMarker(id, ActorIdResolver.resolve(request)));
    }

    @Operation(summary = "마커 숨김")
    @PostMapping("/{id}/unpublish")
    public MarkerDto.MarkerSummaryResponse unpublishMarker(
            @PathVariable UUID id, HttpServletRequest request) {
        return MarkerDto.MarkerSummaryResponse.from(
                markerService.unpublishMarker(id, ActorIdResolver.resolve(request)));
    }

    @Operation(summary = "마커 삭제 (소프트)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMarker(@PathVariable UUID id, HttpServletRequest request) {
        markerService.deleteMarker(id, ActorIdResolver.resolve(request));
    }

    @Operation(summary = "마커 일괄 발행")
    @PostMapping("/batch-publish")
    public BatchPublishResult batchPublishMarkers(
            @RequestBody MarkerDto.BatchPublishRequest req, HttpServletRequest request) {
        int count = markerService.batchPublishMarkers(req.ids(), ActorIdResolver.resolve(request));
        return new BatchPublishResult(count);
    }

    record BatchPublishResult(int publishedCount) {}
}
