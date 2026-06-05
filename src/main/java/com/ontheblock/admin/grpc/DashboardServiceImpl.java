package com.ontheblock.admin.grpc;

import com.ontheblock.admin.domain.marker.MarkerPublicationEventRepository;
import com.ontheblock.admin.grpc.mapper.ProtoMapper;
import com.ontheblock.admin.service.ChangeRequestService;
import com.ontheblock.admin.client.AuthServiceClient;
import com.ontheblock.admin.v1.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.PageRequest;

@GrpcService
@RequiredArgsConstructor
public class DashboardServiceImpl extends DashboardServiceGrpc.DashboardServiceImplBase {

    private final MarkerPublicationEventRepository publicationEventRepository;
    private final ChangeRequestService changeRequestService;
    private final AuthServiceClient authServiceClient;

    @Override
    public void getDashboardStats(GetDashboardStatsRequest req,
                                   StreamObserver<GetDashboardStatsResponse> obs) {
        try {
            int pendingEventCount = publicationEventRepository.countByConsumedAtIsNull();
            int pendingCrCount = changeRequestService.countPending();
            int activeManagerCount = authServiceClient.countActiveManagers();

            var recentEvents = publicationEventRepository
                    .findAllWithFilters(null, null, false, PageRequest.of(0, 5));

            GetDashboardStatsResponse.Builder resp = GetDashboardStatsResponse.newBuilder()
                    .setPendingEventCount(pendingEventCount)
                    .setPendingChangeRequestCount(pendingCrCount)
                    .setActiveManagerCount(activeManagerCount);
            recentEvents.getContent().forEach(e ->
                    resp.addRecentPublications(ProtoMapper.toPublicationEventSummary(e, null)));

            obs.onNext(resp.build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }
}
