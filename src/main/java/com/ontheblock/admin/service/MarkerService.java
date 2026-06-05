package com.ontheblock.admin.service;

import ch.hsr.geohash.GeoHash;
import com.ontheblock.admin.domain.marker.MarkerLayerRepository;
import com.ontheblock.admin.domain.marker.MarkerPublicationEventRepository;
import com.ontheblock.admin.domain.marker.MarkerRepository;
import com.ontheblock.admin.domain.marker.entity.MarkerEntity;
import com.ontheblock.admin.domain.marker.entity.MarkerPublicationEventEntity;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarkerService {

    private static final int GEOHASH_PRECISION = 9;

    private final MarkerRepository markerRepository;
    private final MarkerLayerRepository markerLayerRepository;
    private final MarkerPublicationEventRepository publicationEventRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<MarkerEntity> listMarkers(String layerCode, MarkerEntity.Visibility visibility,
                                          String labelSearch, String geohashPrefix,
                                          UUID placeRef, int page, int pageSize) {
        return markerRepository.findAllWithFilters(
                layerCode, visibility, labelSearch, geohashPrefix, placeRef,
                PageRequest.of(page - 1, pageSize));
    }

    @Transactional(readOnly = true)
    public MarkerEntity getMarker(UUID id) {
        return markerRepository.findById(id)
                .orElseThrow(() -> Status.NOT_FOUND.withDescription("Marker not found").asRuntimeException());
    }

    @Transactional
    public MarkerEntity createMarker(String layerCode, String label, double lat, double lng,
                                     String iconKey, MarkerEntity.Visibility visibility,
                                     UUID placeRef, String filterJson) {
        if (!markerLayerRepository.existsById(layerCode)) {
            throw Status.NOT_FOUND.withDescription("Layer not found: " + layerCode).asRuntimeException();
        }
        String geohash = GeoHash.geoHashStringWithCharacterPrecision(lat, lng, GEOHASH_PRECISION);
        return markerRepository.save(MarkerEntity.builder()
                .layerCode(layerCode)
                .label(label)
                .lat(lat)
                .lng(lng)
                .geohash(geohash)
                .iconKey(iconKey)
                .visibility(visibility != null ? visibility : MarkerEntity.Visibility.VISIBLE)
                .placeRef(placeRef)
                .filterJson(filterJson)
                .build());
    }

    @Transactional
    public MarkerEntity updateMarker(UUID id, String layerCode, String label, double lat, double lng,
                                     String iconKey, MarkerEntity.Visibility visibility,
                                     UUID placeRef, String filterJson) {
        MarkerEntity marker = getMarker(id);
        String geohash = GeoHash.geoHashStringWithCharacterPrecision(lat, lng, GEOHASH_PRECISION);
        marker.update(layerCode, label, lat, lng, geohash, iconKey, visibility, placeRef, filterJson);
        return markerRepository.save(marker);
    }

    @Transactional
    public MarkerEntity publishMarker(UUID id, UUID actorId) {
        MarkerEntity marker = getMarker(id);
        marker.publish();
        markerRepository.save(marker);
        recordPublicationEvent(marker.getId(), MarkerPublicationEventEntity.EventType.PUBLISHED);
        auditLogService.log(actorId, "PUBLISH_MARKER", "MARKER", id, null);
        return marker;
    }

    @Transactional
    public MarkerEntity unpublishMarker(UUID id, UUID actorId) {
        MarkerEntity marker = getMarker(id);
        marker.unpublish();
        markerRepository.save(marker);
        recordPublicationEvent(marker.getId(), MarkerPublicationEventEntity.EventType.UNPUBLISHED);
        auditLogService.log(actorId, "UNPUBLISH_MARKER", "MARKER", id, null);
        return marker;
    }

    @Transactional
    public void deleteMarker(UUID id, UUID actorId) {
        MarkerEntity marker = getMarker(id);
        marker.softDelete();
        markerRepository.save(marker);
        auditLogService.log(actorId, "DELETE_MARKER", "MARKER", id, null);
    }

    @Transactional
    public int batchPublishMarkers(List<UUID> ids, UUID actorId) {
        int count = 0;
        for (UUID id : ids) {
            try {
                publishMarker(id, actorId);
                count++;
            } catch (Exception ignored) {
            }
        }
        return count;
    }

    @Transactional
    public void publishMarkersByPlaceRef(UUID placeRef, UUID actorId) {
        List<MarkerEntity> markers = markerRepository.findAllByPlaceRefAndDeletedAtIsNull(placeRef);
        for (MarkerEntity marker : markers) {
            marker.publish();
            markerRepository.save(marker);
            recordPublicationEvent(marker.getId(), MarkerPublicationEventEntity.EventType.PUBLISHED);
        }
        if (!markers.isEmpty()) {
            auditLogService.log(actorId, "REPUBLISH_MARKERS_BY_PLACE", "PLACE", placeRef, null);
        }
    }

    @Transactional
    public MarkerPublicationEventEntity retriggerPublicationEvent(UUID eventId) {
        MarkerPublicationEventEntity event = publicationEventRepository.findById(eventId)
                .orElseThrow(() -> Status.NOT_FOUND.withDescription("Publication event not found").asRuntimeException());
        event.resetConsumed();
        return publicationEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Page<MarkerPublicationEventEntity> listPublicationEvents(UUID markerId,
                                                                     MarkerPublicationEventEntity.EventType eventType,
                                                                     boolean pendingOnly,
                                                                     int page, int pageSize) {
        return publicationEventRepository.findAllWithFilters(
                markerId, eventType, pendingOnly, PageRequest.of(page - 1, pageSize));
    }

    private void recordPublicationEvent(UUID markerId, MarkerPublicationEventEntity.EventType type) {
        publicationEventRepository.save(MarkerPublicationEventEntity.builder()
                .markerId(markerId)
                .eventType(type)
                .build());
    }
}
