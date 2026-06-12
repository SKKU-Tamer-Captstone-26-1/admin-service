package com.ontheblock.admin.domain.marker.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MarkerPublicationEventTypeConverter
        implements AttributeConverter<MarkerPublicationEventEntity.EventType, String> {

    @Override
    public String convertToDatabaseColumn(MarkerPublicationEventEntity.EventType attribute) {
        if (attribute == null) return null;
        return switch (attribute) {
            case MARKER_PUBLISHED     -> "marker_published";
            case MARKER_HIDDEN        -> "marker_hidden";
            case MARKER_MOVED         -> "marker_moved";
            case MARKER_LAYER_CHANGED -> "marker_layer_changed";
            case MARKER_DELETED       -> "marker_deleted";
        };
    }

    @Override
    public MarkerPublicationEventEntity.EventType convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return switch (dbData) {
            case "marker_published"     -> MarkerPublicationEventEntity.EventType.MARKER_PUBLISHED;
            case "marker_hidden"        -> MarkerPublicationEventEntity.EventType.MARKER_HIDDEN;
            case "marker_moved"         -> MarkerPublicationEventEntity.EventType.MARKER_MOVED;
            case "marker_layer_changed" -> MarkerPublicationEventEntity.EventType.MARKER_LAYER_CHANGED;
            case "marker_deleted"       -> MarkerPublicationEventEntity.EventType.MARKER_DELETED;
            // legacy uppercase tolerance
            case "PUBLISHED"            -> MarkerPublicationEventEntity.EventType.MARKER_PUBLISHED;
            case "UNPUBLISHED"          -> MarkerPublicationEventEntity.EventType.MARKER_HIDDEN;
            default                     -> MarkerPublicationEventEntity.EventType.MARKER_PUBLISHED;
        };
    }
}
