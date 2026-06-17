package com.ontheblock.admin.domain.store.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "products", schema = "admin")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "place_id", nullable = false)
    private UUID placeId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "in_stock", nullable = false)
    @Builder.Default
    private boolean inStock = true;

    @Column(name = "price")
    private Integer price;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Column(name = "beverage_catalog_ref")
    private String beverageCatalogRef;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public void applyChanges(String name, String category, String imageUrl,
                             boolean inStock, Integer price, int displayOrder,
                             String beverageCatalogRef) {
        this.name = name;
        this.category = category;
        this.imageUrl = imageUrl;
        this.inStock = inStock;
        this.price = price;
        this.displayOrder = displayOrder;
        this.beverageCatalogRef = beverageCatalogRef;
        this.updatedAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
