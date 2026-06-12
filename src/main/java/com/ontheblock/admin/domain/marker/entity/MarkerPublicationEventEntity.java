package com.ontheblock.admin.domain.marker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "marker_publication_events", schema = "map_view")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MarkerPublicationEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "marker_id")
    private UUID markerId;

    @Convert(converter = MarkerPublicationEventTypeConverter.class)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    @Column(name = "place_ref", nullable = false)
    private UUID placeRef;

    @Column(name = "published_revision", nullable = false)
    private int publishedRevision;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", columnDefinition = "jsonb")
    private String payloadJson;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void resetConsumed() {
        this.consumedAt = null;
    }

    public enum EventType {
        MARKER_PUBLISHED, MARKER_HIDDEN, MARKER_MOVED, MARKER_LAYER_CHANGED, MARKER_DELETED
    }
}
