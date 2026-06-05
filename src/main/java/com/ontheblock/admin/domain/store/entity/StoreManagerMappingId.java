package com.ontheblock.admin.domain.store.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StoreManagerMappingId implements Serializable {
    private UUID placeId;
    private UUID userId;
}
