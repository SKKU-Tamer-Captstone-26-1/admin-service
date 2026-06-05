package com.ontheblock.admin.grpc;

import com.ontheblock.admin.domain.marker.entity.MarkerEntity;
import com.ontheblock.admin.domain.marker.entity.MarkerPublicationEventEntity;
import com.ontheblock.admin.grpc.mapper.ProtoMapper;
import com.ontheblock.admin.service.MarkerService;
import com.ontheblock.admin.v1.*;
import com.ontheblock.common.v1.PaginationResponse;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;

import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class MarkerAdminServiceImpl extends MarkerAdminServiceGrpc.MarkerAdminServiceImplBase {

    private final MarkerService markerService;

    @Override
    public void listMarkers(ListMarkersRequest req, StreamObserver<ListMarkersResponse> obs) {
        try {
            int page = req.getPagination().getPage() > 0 ? req.getPagination().getPage() : 1;
            int pageSize = req.getPagination().getPageSize() > 0 ? req.getPagination().getPageSize() : 20;

            MarkerEntity.Visibility visibility = req.getVisibilityFilter() == MarkerVisibility.MARKER_VISIBILITY_UNSPECIFIED
                    ? null : ProtoMapper.toVisibilityEntity(req.getVisibilityFilter());
            UUID placeRef = req.getPlaceRef().isBlank() ? null : UUID.fromString(req.getPlaceRef());

            Page<MarkerEntity> result = markerService.listMarkers(
                    req.getLayerCode().isBlank() ? null : req.getLayerCode(),
                    visibility,
                    req.getLabelSearch().isBlank() ? null : req.getLabelSearch(),
                    req.getGeohashPrefix().isBlank() ? null : req.getGeohashPrefix(),
                    placeRef, page, pageSize);

            ListMarkersResponse.Builder resp = ListMarkersResponse.newBuilder()
                    .setPagination(PaginationResponse.newBuilder()
                            .setTotalCount((int) result.getTotalElements())
                            .setPage(page)
                            .setPageSize(pageSize)
                            .setHasNext(result.hasNext())
                            .build());
            result.getContent().forEach(m -> resp.addMarkers(ProtoMapper.toMarkerSummary(m)));
            obs.onNext(resp.build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void getMarker(GetMarkerRequest req, StreamObserver<GetMarkerResponse> obs) {
        try {
            MarkerEntity marker = markerService.getMarker(UUID.fromString(req.getId()));
            obs.onNext(GetMarkerResponse.newBuilder()
                    .setMarker(ProtoMapper.toMarkerDetail(marker))
                    .build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void createMarker(CreateMarkerRequest req, StreamObserver<CreateMarkerResponse> obs) {
        try {
            UUID placeRef = req.getPlaceRef().isBlank() ? null : UUID.fromString(req.getPlaceRef());
            MarkerEntity marker = markerService.createMarker(
                    req.getLayerCode(), req.getLabel(), req.getLat(), req.getLng(),
                    req.getIconKey().isBlank() ? null : req.getIconKey(),
                    ProtoMapper.toVisibilityEntity(req.getVisibility()),
                    placeRef,
                    req.getFilterJson().isEmpty() ? null : req.getFilterJson().toStringUtf8());
            obs.onNext(CreateMarkerResponse.newBuilder()
                    .setMarker(ProtoMapper.toMarkerDetail(marker))
                    .build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void updateMarker(UpdateMarkerRequest req, StreamObserver<UpdateMarkerResponse> obs) {
        try {
            UUID placeRef = req.getPlaceRef().isBlank() ? null : UUID.fromString(req.getPlaceRef());
            MarkerEntity marker = markerService.updateMarker(
                    UUID.fromString(req.getId()),
                    req.getLayerCode(), req.getLabel(), req.getLat(), req.getLng(),
                    req.getIconKey().isBlank() ? null : req.getIconKey(),
                    ProtoMapper.toVisibilityEntity(req.getVisibility()),
                    placeRef,
                    req.getFilterJson().isEmpty() ? null : req.getFilterJson().toStringUtf8());
            obs.onNext(UpdateMarkerResponse.newBuilder()
                    .setMarker(ProtoMapper.toMarkerDetail(marker))
                    .build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void publishMarker(PublishMarkerRequest req, StreamObserver<PublishMarkerResponse> obs) {
        try {
            // actorId injected from gRPC metadata by gateway; using a placeholder UUID here
            // Gateway must inject x-user-id metadata which admin-service should extract
            MarkerEntity marker = markerService.publishMarker(
                    UUID.fromString(req.getId()), extractActorId());
            obs.onNext(PublishMarkerResponse.newBuilder()
                    .setMarker(ProtoMapper.toMarkerDetail(marker))
                    .build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void unpublishMarker(UnpublishMarkerRequest req, StreamObserver<UnpublishMarkerResponse> obs) {
        try {
            MarkerEntity marker = markerService.unpublishMarker(
                    UUID.fromString(req.getId()), extractActorId());
            obs.onNext(UnpublishMarkerResponse.newBuilder()
                    .setMarker(ProtoMapper.toMarkerDetail(marker))
                    .build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void deleteMarker(DeleteMarkerRequest req, StreamObserver<DeleteMarkerResponse> obs) {
        try {
            markerService.deleteMarker(UUID.fromString(req.getId()), extractActorId());
            obs.onNext(DeleteMarkerResponse.newBuilder().build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void batchPublishMarkers(BatchPublishMarkersRequest req, StreamObserver<BatchPublishMarkersResponse> obs) {
        try {
            int count = markerService.batchPublishMarkers(
                    req.getIdsList().stream().map(UUID::fromString).toList(),
                    extractActorId());
            obs.onNext(BatchPublishMarkersResponse.newBuilder().setPublishedCount(count).build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void listPublicationEvents(ListPublicationEventsRequest req,
                                       StreamObserver<ListPublicationEventsResponse> obs) {
        try {
            int page = req.getPagination().getPage() > 0 ? req.getPagination().getPage() : 1;
            int pageSize = req.getPagination().getPageSize() > 0 ? req.getPagination().getPageSize() : 20;

            UUID markerId = req.getMarkerId().isBlank() ? null : UUID.fromString(req.getMarkerId());
            MarkerPublicationEventEntity.EventType eventType =
                    req.getEventTypeFilter() == PublicationEventType.PUBLICATION_EVENT_TYPE_UNSPECIFIED
                            ? null : ProtoMapper.toEventTypeEntity(req.getEventTypeFilter());

            Page<MarkerPublicationEventEntity> result = markerService.listPublicationEvents(
                    markerId, eventType, req.getPendingOnly(), page, pageSize);

            ListPublicationEventsResponse.Builder resp = ListPublicationEventsResponse.newBuilder()
                    .setPagination(PaginationResponse.newBuilder()
                            .setTotalCount((int) result.getTotalElements())
                            .setPage(page).setPageSize(pageSize).setHasNext(result.hasNext()).build())
                    .setTotalPendingCount(result.getTotalElements() > 0
                            ? (int) result.getContent().stream().filter(e -> e.getConsumedAt() == null).count()
                            : 0);
            result.getContent().forEach(e -> resp.addEvents(ProtoMapper.toPublicationEventSummary(e, null)));
            obs.onNext(resp.build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void retriggerPublicationEvent(RetriggerPublicationEventRequest req,
                                           StreamObserver<RetriggerPublicationEventResponse> obs) {
        try {
            var event = markerService.retriggerPublicationEvent(UUID.fromString(req.getId()));
            obs.onNext(RetriggerPublicationEventResponse.newBuilder()
                    .setEvent(ProtoMapper.toPublicationEventSummary(event, null))
                    .build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    // Gateway injects x-user-id via metadata; extracted via GrpcContextUtil (placeholder)
    private UUID extractActorId() {
        return GrpcContextUtil.getUserId();
    }
}
