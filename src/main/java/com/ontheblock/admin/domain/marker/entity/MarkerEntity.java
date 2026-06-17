package com.ontheblock.admin.domain.marker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "markers", schema = "map_view")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MarkerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "layer_code", nullable = false, length = 50)
    private String layerCode;

    @Column(name = "place_ref", nullable = false)
    private UUID placeRef;

    @Column(name = "label", nullable = false, length = 200)
    private String label;

    @Column(name = "lat", nullable = false)
    private double lat;

    @Column(name = "lng", nullable = false)
    private double lng;

    @Column(name = "geohash", nullable = false, length = 12)
    private String geohash;

    @Column(name = "icon_key", length = 100)
    private String iconKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    @Builder.Default
    private Visibility visibility = Visibility.VISIBLE;

    @Column(name = "filter_json", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    @Builder.Default
    private String filterJson = "{}";

    @Column(name = "published_revision", nullable = false)
    @Builder.Default
    private int publishedRevision = 0;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public void update(String layerCode, String label, double lat, double lng, String geohash,
                       String iconKey, Visibility visibility, UUID placeRef, String filterJson) {
        this.layerCode = layerCode;
        this.label = label;
        this.lat = lat;
        this.lng = lng;
        this.geohash = geohash;
        this.iconKey = iconKey;
        this.visibility = visibility;
        this.placeRef = placeRef;
        this.filterJson = filterJson != null ? filterJson : "{}";
        this.updatedAt = LocalDateTime.now();
    }

    public void updateFilterJson(String filterJson) {
        this.filterJson = filterJson != null ? filterJson : "{}";
        this.updatedAt = LocalDateTime.now();
    }

    public void publish() {
        this.publishedRevision++;
        this.updatedAt = LocalDateTime.now();
    }

    public void unpublish() {
        this.visibility = Visibility.HIDDEN;
        this.updatedAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public enum Visibility {
        VISIBLE, HIDDEN
    }
}
