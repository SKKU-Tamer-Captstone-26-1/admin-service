package com.ontheblock.admin.controller;

import com.ontheblock.admin.client.AuthServiceClient;
import com.ontheblock.admin.domain.marker.MarkerPublicationEventRepository;
import com.ontheblock.admin.dto.MarkerDto;
import com.ontheblock.admin.service.ChangeRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Dashboard", description = "운영자 홈 대시보드 통계")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final MarkerPublicationEventRepository publicationEventRepository;
    private final ChangeRequestService changeRequestService;
    private final AuthServiceClient authServiceClient;

    @Operation(summary = "대시보드 통계 조회")
    @GetMapping
    public DashboardStats getStats() {
        int pendingEvents = publicationEventRepository.countByConsumedAtIsNull();
        int pendingCr = changeRequestService.countPending();
        int activeManagers = authServiceClient.countActiveManagers();
        List<MarkerDto.PublicationEventResponse> recent = publicationEventRepository
                .findAllWithFilters(null, null, false, PageRequest.of(0, 5))
                .stream()
                .map(MarkerDto.PublicationEventResponse::from)
                .toList();
        return new DashboardStats(pendingEvents, pendingCr, activeManagers, recent);
    }

    record DashboardStats(
            int pendingEventCount,
            int pendingChangeRequestCount,
            int activeManagerCount,
            List<MarkerDto.PublicationEventResponse> recentPublications
    ) {}
}
