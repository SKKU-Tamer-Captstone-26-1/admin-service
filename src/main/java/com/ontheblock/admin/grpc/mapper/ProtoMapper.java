package com.ontheblock.admin.grpc.mapper;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import com.ontheblock.admin.domain.changerequest.entity.ChangeRequestEntity;
import com.ontheblock.admin.domain.marker.entity.MarkerEntity;
import com.ontheblock.admin.domain.marker.entity.MarkerLayerEntity;
import com.ontheblock.admin.domain.marker.entity.MarkerPublicationEventEntity;
import com.ontheblock.admin.domain.store.entity.ProductEntity;
import com.ontheblock.admin.domain.store.entity.StoreEntity;
import com.ontheblock.admin.v1.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public final class ProtoMapper {

    private ProtoMapper() {}

    public static Timestamp toTimestamp(LocalDateTime dt) {
        if (dt == null) return Timestamp.getDefaultInstance();
        long epochSecond = dt.toEpochSecond(ZoneOffset.UTC);
        return Timestamp.newBuilder().setSeconds(epochSecond).build();
    }

    public static MarkerVisibility toVisibilityProto(MarkerEntity.Visibility v) {
        if (v == null) return MarkerVisibility.MARKER_VISIBILITY_UNSPECIFIED;
        return switch (v) {
            case VISIBLE -> MarkerVisibility.MARKER_VISIBILITY_VISIBLE;
            case HIDDEN  -> MarkerVisibility.MARKER_VISIBILITY_HIDDEN;
        };
    }

    public static MarkerEntity.Visibility toVisibilityEntity(MarkerVisibility v) {
        return switch (v) {
            case MARKER_VISIBILITY_HIDDEN -> MarkerEntity.Visibility.HIDDEN;
            default -> MarkerEntity.Visibility.VISIBLE;
        };
    }

    public static PublicationEventType toEventTypeProto(MarkerPublicationEventEntity.EventType t) {
        if (t == null) return PublicationEventType.PUBLICATION_EVENT_TYPE_UNSPECIFIED;
        return switch (t) {
            case MARKER_PUBLISHED     -> PublicationEventType.PUBLICATION_EVENT_TYPE_PUBLISHED;
            case MARKER_HIDDEN        -> PublicationEventType.PUBLICATION_EVENT_TYPE_HIDDEN;
            case MARKER_MOVED         -> PublicationEventType.PUBLICATION_EVENT_TYPE_MOVED;
            case MARKER_LAYER_CHANGED -> PublicationEventType.PUBLICATION_EVENT_TYPE_LAYER_CHANGED;
            case MARKER_DELETED       -> PublicationEventType.PUBLICATION_EVENT_TYPE_DELETED;
        };
    }

    public static MarkerPublicationEventEntity.EventType toEventTypeEntity(PublicationEventType t) {
        return switch (t) {
            case PUBLICATION_EVENT_TYPE_UNPUBLISHED, PUBLICATION_EVENT_TYPE_HIDDEN ->
                    MarkerPublicationEventEntity.EventType.MARKER_HIDDEN;
            case PUBLICATION_EVENT_TYPE_MOVED ->
                    MarkerPublicationEventEntity.EventType.MARKER_MOVED;
            case PUBLICATION_EVENT_TYPE_LAYER_CHANGED ->
                    MarkerPublicationEventEntity.EventType.MARKER_LAYER_CHANGED;
            case PUBLICATION_EVENT_TYPE_DELETED ->
                    MarkerPublicationEventEntity.EventType.MARKER_DELETED;
            default -> MarkerPublicationEventEntity.EventType.MARKER_PUBLISHED;
        };
    }

    public static MarkerSummary toMarkerSummary(MarkerEntity e) {
        MarkerSummary.Builder b = MarkerSummary.newBuilder()
                .setId(e.getId().toString())
                .setLayerCode(e.getLayerCode())
                .setLabel(e.getLabel())
                .setLat(e.getLat())
                .setLng(e.getLng())
                .setGeohash(e.getGeohash())
                .setVisibility(toVisibilityProto(e.getVisibility()))
                .setPublishedRevision(e.getPublishedRevision())
                .setCreatedAt(toTimestamp(e.getCreatedAt()))
                .setUpdatedAt(toTimestamp(e.getUpdatedAt()));
        if (e.getPlaceRef() != null) b.setPlaceRef(e.getPlaceRef().toString());
        return b.build();
    }

    public static MarkerDetail toMarkerDetail(MarkerEntity e) {
        MarkerDetail.Builder b = MarkerDetail.newBuilder()
                .setId(e.getId().toString())
                .setLayerCode(e.getLayerCode())
                .setLabel(e.getLabel())
                .setLat(e.getLat())
                .setLng(e.getLng())
                .setGeohash(e.getGeohash())
                .setVisibility(toVisibilityProto(e.getVisibility()))
                .setPublishedRevision(e.getPublishedRevision())
                .setCreatedAt(toTimestamp(e.getCreatedAt()))
                .setUpdatedAt(toTimestamp(e.getUpdatedAt()))
                .setDeletedAt(toTimestamp(e.getDeletedAt()));
        if (e.getPlaceRef() != null)  b.setPlaceRef(e.getPlaceRef().toString());
        if (e.getIconKey() != null)   b.setIconKey(e.getIconKey());
        if (e.getFilterJson() != null) b.setFilterJson(ByteString.copyFromUtf8(e.getFilterJson()));
        return b.build();
    }

    public static LayerSummary toLayerSummary(MarkerLayerEntity e, int markerCount) {
        LayerSummary.Builder b = LayerSummary.newBuilder()
                .setCode(e.getCode())
                .setLabelKo(e.getLabelKo())
                .setLabelEn(e.getLabelEn())
                .setDisplayOrder(e.getDisplayOrder())
                .setDefaultVisible(e.isDefaultVisible())
                .setIsActive(e.isActive())
                .setMarkerCount(markerCount);
        if (e.getIconKey() != null) b.setIconKey(e.getIconKey());
        return b.build();
    }

    public static PublicationEventSummary toPublicationEventSummary(MarkerPublicationEventEntity e,
                                                                      String markerLabel) {
        PublicationEventSummary.Builder b = PublicationEventSummary.newBuilder()
                .setId(e.getId().toString())
                .setMarkerLabel(markerLabel != null ? markerLabel : "")
                .setEventType(toEventTypeProto(e.getEventType()))
                .setCreatedAt(toTimestamp(e.getCreatedAt()))
                .setConsumedAt(toTimestamp(e.getConsumedAt()));
        if (e.getMarkerId() != null) b.setMarkerId(e.getMarkerId().toString());
        if (e.getPayloadJson() != null) b.setPayloadJson(ByteString.copyFromUtf8(e.getPayloadJson()));
        b.setPublishedRevision(e.getPublishedRevision());
        if (e.getPlaceRef() != null) b.setPlaceRef(e.getPlaceRef().toString());
        return b.build();
    }

    public static ChangeRequestSummary toChangeRequestSummary(ChangeRequestEntity e) {
        ChangeRequestSummary.Builder b = ChangeRequestSummary.newBuilder()
                .setId(e.getId().toString())
                .setRequesterId(e.getRequesterId().toString())
                .setTargetType(toTargetTypeProto(e.getTargetType()))
                .setTargetRef(e.getTargetRef().toString())
                .setStatus(toStatusProto(e.getStatus()))
                .setCreatedAt(toTimestamp(e.getCreatedAt()))
                .setUpdatedAt(toTimestamp(e.getUpdatedAt()));
        if (e.getReviewerId() != null)    b.setReviewerId(e.getReviewerId().toString());
        if (e.getReviewedAt() != null)    b.setReviewedAt(toTimestamp(e.getReviewedAt()));
        if (e.getReviewComment() != null) b.setReviewComment(e.getReviewComment());
        return b.build();
    }

    public static ChangeRequestDetail toChangeRequestDetail(ChangeRequestEntity e, String currentSnapshot) {
        ChangeRequestDetail.Builder b = ChangeRequestDetail.newBuilder()
                .setId(e.getId().toString())
                .setRequesterId(e.getRequesterId().toString())
                .setTargetType(toTargetTypeProto(e.getTargetType()))
                .setTargetRef(e.getTargetRef().toString())
                .setProposedChanges(ByteString.copyFromUtf8(e.getProposedChanges()))
                .setStatus(toStatusProto(e.getStatus()))
                .setCreatedAt(toTimestamp(e.getCreatedAt()))
                .setUpdatedAt(toTimestamp(e.getUpdatedAt()));
        if (currentSnapshot != null)      b.setCurrentSnapshot(ByteString.copyFromUtf8(currentSnapshot));
        if (e.getReviewerId() != null)    b.setReviewerId(e.getReviewerId().toString());
        if (e.getReviewedAt() != null)    b.setReviewedAt(toTimestamp(e.getReviewedAt()));
        if (e.getReviewComment() != null) b.setReviewComment(e.getReviewComment());
        if (e.getAttachments() != null)   b.addAllAttachmentUrls(List.of(e.getAttachments()));
        return b.build();
    }

    public static TargetType toTargetTypeProto(ChangeRequestEntity.TargetType t) {
        return switch (t) {
            case STORE_INFO    -> TargetType.TARGET_TYPE_STORE_INFO;
            case PRODUCT_LIST  -> TargetType.TARGET_TYPE_PRODUCT_LIST;
            case PRODUCT_IMAGE -> TargetType.TARGET_TYPE_PRODUCT_IMAGE;
        };
    }

    public static ChangeRequestEntity.TargetType toTargetTypeEntity(TargetType t) {
        return switch (t) {
            case TARGET_TYPE_PRODUCT_LIST  -> ChangeRequestEntity.TargetType.PRODUCT_LIST;
            case TARGET_TYPE_PRODUCT_IMAGE -> ChangeRequestEntity.TargetType.PRODUCT_IMAGE;
            default                        -> ChangeRequestEntity.TargetType.STORE_INFO;
        };
    }

    public static ChangeRequestStatus toStatusProto(ChangeRequestEntity.Status s) {
        return switch (s) {
            case PENDING   -> ChangeRequestStatus.CHANGE_REQUEST_STATUS_PENDING;
            case APPROVED  -> ChangeRequestStatus.CHANGE_REQUEST_STATUS_APPROVED;
            case REJECTED  -> ChangeRequestStatus.CHANGE_REQUEST_STATUS_REJECTED;
            case WITHDRAWN -> ChangeRequestStatus.CHANGE_REQUEST_STATUS_WITHDRAWN;
        };
    }

    public static ChangeRequestEntity.Status toStatusEntity(ChangeRequestStatus s) {
        return switch (s) {
            case CHANGE_REQUEST_STATUS_APPROVED  -> ChangeRequestEntity.Status.APPROVED;
            case CHANGE_REQUEST_STATUS_REJECTED  -> ChangeRequestEntity.Status.REJECTED;
            case CHANGE_REQUEST_STATUS_WITHDRAWN -> ChangeRequestEntity.Status.WITHDRAWN;
            default                              -> ChangeRequestEntity.Status.PENDING;
        };
    }

    public static StoreSummary toStoreSummary(StoreEntity e, List<String> managerIds) {
        return StoreSummary.newBuilder()
                .setId(e.getId().toString())
                .setType(toStoreTypeProto(e.getType()))
                .setName(e.getName())
                .setAddress(e.getAddress())
                .setStatus(toStoreStatusProto(e.getStatus()))
                .addAllManagerUserIds(managerIds)
                .setCreatedAt(toTimestamp(e.getCreatedAt()))
                .build();
    }

    public static StoreDetail toStoreDetail(StoreEntity e, List<String> managerIds) {
        StoreDetail.Builder b = StoreDetail.newBuilder()
                .setId(e.getId().toString())
                .setType(toStoreTypeProto(e.getType()))
                .setName(e.getName())
                .setAddress(e.getAddress())
                .setLat(e.getLat())
                .setLng(e.getLng())
                .setStatus(toStoreStatusProto(e.getStatus()))
                .addAllManagerUserIds(managerIds)
                .setCreatedAt(toTimestamp(e.getCreatedAt()))
                .setUpdatedAt(toTimestamp(e.getUpdatedAt()))
                .setDeletedAt(toTimestamp(e.getDeletedAt()));
        if (e.getBusinessHours() != null) b.setBusinessHours(ByteString.copyFromUtf8(e.getBusinessHours()));
        if (e.getContact() != null)       b.setContact(e.getContact());
        return b.build();
    }

    public static ProductSummary toProductSummary(ProductEntity e) {
        ProductSummary.Builder b = ProductSummary.newBuilder()
                .setId(e.getId().toString())
                .setPlaceId(e.getPlaceId().toString())
                .setName(e.getName())
                .setInStock(e.isInStock())
                .setDisplayOrder(e.getDisplayOrder());
        if (e.getCategory() != null) b.setCategory(e.getCategory());
        if (e.getImageUrl() != null) b.setImageUrl(e.getImageUrl());
        if (e.getPrice() != null)    b.setPrice(e.getPrice());
        b.setBeverageCatalogRef(e.getBeverageCatalogRef() != null ? e.getBeverageCatalogRef() : "");
        return b.build();
    }

    public static StoreType toStoreTypeProto(StoreEntity.StoreType t) {
        return switch (t) {
            case BAR         -> StoreType.STORE_TYPE_BAR;
            case LIQUOR_SHOP -> StoreType.STORE_TYPE_LIQUOR_SHOP;
        };
    }

    public static StoreEntity.StoreType toStoreTypeEntity(StoreType t) {
        return switch (t) {
            case STORE_TYPE_LIQUOR_SHOP -> StoreEntity.StoreType.LIQUOR_SHOP;
            default                     -> StoreEntity.StoreType.BAR;
        };
    }

    public static StoreStatus toStoreStatusProto(StoreEntity.StoreStatus s) {
        return switch (s) {
            case ACTIVE   -> StoreStatus.STORE_STATUS_ACTIVE;
            case INACTIVE -> StoreStatus.STORE_STATUS_INACTIVE;
        };
    }

    public static StoreEntity.StoreStatus toStoreStatusEntity(StoreStatus s) {
        return switch (s) {
            case STORE_STATUS_INACTIVE -> StoreEntity.StoreStatus.INACTIVE;
            default                    -> StoreEntity.StoreStatus.ACTIVE;
        };
    }
}
