package com.ontheblock.admin.service;

import ch.hsr.geohash.GeoHash;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<MarkerEntity> listMarkers(String layerCode, MarkerEntity.Visibility visibility,
                                          String labelSearch, String geohashPrefix,
                                          UUID placeRef, int page, int pageSize) {
        String visibilityStr = visibility != null ? visibility.name() : null;
        return markerRepository.findAllWithFilters(
                layerCode, visibilityStr, labelSearch, geohashPrefix, placeRef,
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
        if (placeRef == null) {
            throw Status.INVALID_ARGUMENT.withDescription("placeRef is required").asRuntimeException();
        }
        if (!markerLayerRepository.existsById(layerCode)) {
            throw Status.NOT_FOUND.withDescription("Layer not found: " + layerCode).asRuntimeException();
        }
        String geohash = GeoHash.geoHashStringWithCharacterPrecision(lat, lng, GEOHASH_PRECISION);
        String effectiveFilterJson = (filterJson == null || filterJson.isBlank()) ? "{}" : filterJson;
        return markerRepository.save(MarkerEntity.builder()
                .layerCode(layerCode)
                .label(label)
                .lat(lat)
                .lng(lng)
                .geohash(geohash)
                .iconKey(iconKey)
                .visibility(visibility != null ? visibility : MarkerEntity.Visibility.VISIBLE)
                .placeRef(placeRef)
                .filterJson(effectiveFilterJson)
                .build());
    }

    @Transactional
    public MarkerEntity updateMarker(UUID id, String layerCode, String label, double lat, double lng,
                                     String iconKey, MarkerEntity.Visibility visibility,
                                     UUID placeRef, String filterJson) {
        MarkerEntity marker = getMarker(id);
        double oldLat = marker.getLat();
        double oldLng = marker.getLng();
        String oldLayer = marker.getLayerCode();

        String geohash = GeoHash.geoHashStringWithCharacterPrecision(lat, lng, GEOHASH_PRECISION);
        marker.update(layerCode, label, lat, lng, geohash, iconKey, visibility, placeRef, filterJson);
        MarkerEntity saved = markerRepository.save(marker);

        if (saved.getPublishedRevision() > 0) {
            if (oldLat != lat || oldLng != lng) {
                recordPublicationEvent(saved, MarkerPublicationEventEntity.EventType.MARKER_MOVED);
            }
            if (!oldLayer.equals(layerCode)) {
                recordPublicationEvent(saved, MarkerPublicationEventEntity.EventType.MARKER_LAYER_CHANGED);
            }
        }

        return saved;
    }

    @Transactional
    public MarkerEntity publishMarker(UUID id, UUID actorId) {
        MarkerEntity marker = getMarker(id);
        marker.publish();
        markerRepository.save(marker);
        recordPublicationEvent(marker, MarkerPublicationEventEntity.EventType.MARKER_PUBLISHED);
        auditLogService.log(actorId, "PUBLISH_MARKER", "MARKER", id, null);
        return marker;
    }

    @Transactional
    public MarkerEntity unpublishMarker(UUID id, UUID actorId) {
        MarkerEntity marker = getMarker(id);
        marker.unpublish();
        markerRepository.save(marker);
        if (marker.getPublishedRevision() > 0) {
            recordPublicationEvent(marker, MarkerPublicationEventEntity.EventType.MARKER_HIDDEN);
        }
        auditLogService.log(actorId, "UNPUBLISH_MARKER", "MARKER", id, null);
        return marker;
    }

    @Transactional
    public void deleteMarker(UUID id, UUID actorId) {
        MarkerEntity marker = getMarker(id);
        marker.softDelete();
        markerRepository.save(marker);
        if (marker.getPublishedRevision() > 0) {
            recordPublicationEvent(marker, MarkerPublicationEventEntity.EventType.MARKER_DELETED);
        }
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
            recordPublicationEvent(marker, MarkerPublicationEventEntity.EventType.MARKER_PUBLISHED);
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

    // Merges manually-edited inventory items into filter_json.inventory while preserving any
    // existing items whose source is not "manual" (i.e. product-sourced or untagged legacy items).
    @Transactional
    public MarkerEntity updateMarkerInventory(UUID id, List<InventoryItemInput> items) {
        MarkerEntity marker = getMarker(id);

        ArrayNode manualItems = objectMapper.createArrayNode();
        for (InventoryItemInput item : (items != null ? items : List.of())) {
            if (item.nameKo() == null || item.nameKo().isBlank()) {
                continue;
            }
            manualItems.add(buildInventoryItem(item.nameKo(), item.beverageCatalogRef(), "manual"));
        }

        try {
            String existingFilterJson = marker.getFilterJson();
            JsonNode filterJsonNode = (existingFilterJson == null || existingFilterJson.isBlank())
                    ? objectMapper.readTree("{}")
                    : objectMapper.readTree(existingFilterJson);
            ObjectNode filterJson = filterJsonNode instanceof ObjectNode
                    ? (ObjectNode) filterJsonNode
                    : objectMapper.createObjectNode();

            ArrayNode productItems = extractItemsExcludingSource(filterJson.get("inventory"), "manual");

            ArrayNode merged = objectMapper.createArrayNode();
            merged.addAll(productItems);
            merged.addAll(manualItems);

            filterJson.set("inventory", merged);
            marker.updateFilterJson(objectMapper.writeValueAsString(filterJson));
            return markerRepository.save(marker);
        } catch (JsonProcessingException e) {
            throw Status.INTERNAL
                    .withDescription("Failed to update marker filter_json.inventory for id=" + id)
                    .withCause(e)
                    .asRuntimeException();
        }
    }

    public record InventoryItemInput(String nameKo, String beverageCatalogRef) {}

    // Builds a single inventory item node using the shared snake_case contract
    // (name_ko, optional beverage_catalog_ref, source tag).
    private ObjectNode buildInventoryItem(String nameKo, String beverageCatalogRef, String source) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("name_ko", nameKo);
        if (beverageCatalogRef != null && !beverageCatalogRef.isBlank()) {
            node.put("beverage_catalog_ref", beverageCatalogRef);
        }
        node.put("source", source);
        return node;
    }

    // Returns the subset of an existing inventory array whose "source" field is NOT the given
    // value (kept as-is, untouched). Items without a "source" field are treated as not matching
    // "manual" and are therefore preserved (i.e. legacy untagged items are kept on the product side).
    private ArrayNode extractItemsExcludingSource(JsonNode existingInventory, String excludedSource) {
        ArrayNode result = objectMapper.createArrayNode();
        if (existingInventory == null || !existingInventory.isArray()) {
            return result;
        }
        for (JsonNode item : existingInventory) {
            if (!item.path("source").asText("").equals(excludedSource)) {
                result.add(item);
            }
        }
        return result;
    }

    private void recordPublicationEvent(MarkerEntity marker, MarkerPublicationEventEntity.EventType type) {
        publicationEventRepository.save(MarkerPublicationEventEntity.builder()
                .markerId(marker.getId())
                .placeRef(marker.getPlaceRef())
                .publishedRevision(marker.getPublishedRevision())
                .eventType(type)
                .payloadJson(buildCanonicalPayload(marker))
                .build());
    }

    private String buildCanonicalPayload(MarkerEntity m) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("place_ref", m.getPlaceRef() != null ? m.getPlaceRef().toString() : null);
            node.put("layer_code", m.getLayerCode());
            node.put("label", m.getLabel());
            node.put("lat", m.getLat());
            node.put("lng", m.getLng());
            node.put("geohash", m.getGeohash());
            if (m.getIconKey() != null) {
                node.put("icon_key", m.getIconKey());
            } else {
                node.putNull("icon_key");
            }
            node.put("visibility", m.getVisibility().name().toLowerCase());
            String rawFilterJson = m.getFilterJson() == null ? "{}" : m.getFilterJson();
            node.set("filter_json", objectMapper.readTree(rawFilterJson));
            node.put("published_revision", m.getPublishedRevision());
            node.put("published_at", m.getUpdatedAt() != null ? m.getUpdatedAt().toString() : null);
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw Status.INTERNAL.withCause(e).asRuntimeException();
        }
    }
}
