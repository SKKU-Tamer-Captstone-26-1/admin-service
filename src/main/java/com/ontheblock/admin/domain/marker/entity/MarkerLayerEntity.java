package com.ontheblock.admin.domain.marker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "marker_layers", schema = "map_view")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MarkerLayerEntity {

    @Id
    @Column(name = "code", length = 50)
    private String code;

    @Column(name = "label_ko", nullable = false, length = 100)
    private String labelKo;

    @Column(name = "label_en", nullable = false, length = 100)
    private String labelEn;

    @Column(name = "icon_key", length = 100)
    private String iconKey;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Column(name = "default_visible", nullable = false)
    @Builder.Default
    private boolean defaultVisible = true;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public void update(String labelKo, String labelEn, String iconKey, boolean defaultVisible, boolean isActive) {
        this.labelKo = labelKo;
        this.labelEn = labelEn;
        this.iconKey = iconKey;
        this.defaultVisible = defaultVisible;
        this.isActive = isActive;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
        this.updatedAt = LocalDateTime.now();
    }
}
