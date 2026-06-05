package com.ontheblock.admin.domain.store.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "place_manager_mappings", schema = "admin")
@IdClass(StoreManagerMappingId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StoreManagerMappingEntity {

    @Id
    @Column(name = "place_id")
    private UUID placeId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
