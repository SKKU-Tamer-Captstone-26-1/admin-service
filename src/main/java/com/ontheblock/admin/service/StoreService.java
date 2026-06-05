package com.ontheblock.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontheblock.admin.domain.store.ProductRepository;
import com.ontheblock.admin.domain.store.StoreManagerMappingRepository;
import com.ontheblock.admin.domain.store.StoreRepository;
import com.ontheblock.admin.domain.store.entity.ProductEntity;
import com.ontheblock.admin.domain.store.entity.StoreEntity;
import com.ontheblock.admin.domain.store.entity.StoreManagerMappingEntity;
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
public class StoreService {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final StoreManagerMappingRepository mappingRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<StoreEntity> listStores(StoreEntity.StoreType type, StoreEntity.StoreStatus status,
                                         String nameSearch, String managerUserId, int page, int pageSize) {
        if (managerUserId != null && !managerUserId.isBlank()) {
            return storeRepository.findAllByManagerUserId(UUID.fromString(managerUserId), PageRequest.of(page - 1, pageSize));
        }
        return storeRepository.findAllWithFilters(type, status, nameSearch, PageRequest.of(page - 1, pageSize));
    }

    @Transactional(readOnly = true)
    public StoreEntity getStore(UUID id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> Status.NOT_FOUND.withDescription("Store not found").asRuntimeException());
    }

    @Transactional(readOnly = true)
    public List<ProductEntity> listProducts(UUID placeId) {
        return productRepository.findAllByPlaceIdAndDeletedAtIsNullOrderByDisplayOrderAsc(placeId);
    }

    @Transactional(readOnly = true)
    public List<UUID> getManagerIds(UUID placeId) {
        return mappingRepository.findAllByPlaceId(placeId).stream()
                .map(StoreManagerMappingEntity::getUserId)
                .toList();
    }

    @Transactional
    public StoreEntity createStore(StoreEntity.StoreType type, String name, String address,
                                    double lat, double lng, String businessHours, String contact,
                                    UUID actorId) {
        StoreEntity store = storeRepository.save(StoreEntity.builder()
                .type(type)
                .name(name)
                .address(address)
                .lat(lat)
                .lng(lng)
                .businessHours(businessHours)
                .contact(contact)
                .build());
        auditLogService.log(actorId, "CREATE_STORE", "STORE", store.getId(), null);
        return store;
    }

    @Transactional
    public void deleteStore(UUID id, UUID actorId) {
        StoreEntity store = getStore(id);
        store.softDelete();
        storeRepository.save(store);
        auditLogService.log(actorId, "DELETE_STORE", "STORE", id, null);
    }

    @Transactional
    public void assignManager(UUID placeId, UUID userId) {
        getStore(placeId);
        StoreManagerMappingEntity mapping = StoreManagerMappingEntity.builder()
                .placeId(placeId)
                .userId(userId)
                .build();
        mappingRepository.save(mapping);
    }

    @Transactional
    public void removeManager(UUID placeId, UUID userId) {
        mappingRepository.deleteByPlaceIdAndUserId(placeId, userId);
    }

    @Transactional(readOnly = true)
    public boolean isManagerOfStore(UUID userId, UUID placeId) {
        return mappingRepository.findAllByUserId(userId).stream()
                .anyMatch(m -> m.getPlaceId().equals(placeId));
    }

    // Called by ChangeRequestService upon approval
    @Transactional
    public void applyStoreInfoChanges(UUID placeId, String proposedChangesJson) {
        StoreEntity store = getStore(placeId);
        try {
            JsonNode changes = objectMapper.readTree(proposedChangesJson);
            store.applyChanges(
                    changes.path("name").asText(store.getName()),
                    changes.path("address").asText(store.getAddress()),
                    changes.path("lat").asDouble(store.getLat()),
                    changes.path("lng").asDouble(store.getLng()),
                    changes.has("business_hours") ? changes.get("business_hours").toString() : store.getBusinessHours(),
                    changes.path("contact").asText(store.getContact())
            );
            storeRepository.save(store);
        } catch (JsonProcessingException e) {
            throw Status.INVALID_ARGUMENT.withDescription("Invalid proposed_changes JSON").asRuntimeException();
        }
    }

    @Transactional
    public void applyProductListChanges(UUID placeId, String proposedChangesJson) {
        try {
            JsonNode changes = objectMapper.readTree(proposedChangesJson);
            // upsert products from the proposed list
            if (changes.has("products")) {
                for (JsonNode p : changes.get("products")) {
                    String productIdStr = p.path("id").asText("");
                    if (!productIdStr.isBlank()) {
                        UUID productId = UUID.fromString(productIdStr);
                        productRepository.findById(productId).ifPresent(product -> {
                            product.applyChanges(
                                    p.path("name").asText(product.getName()),
                                    p.path("category").asText(product.getCategory()),
                                    p.path("image_url").asText(product.getImageUrl()),
                                    p.path("in_stock").asBoolean(product.isInStock()),
                                    p.has("price") ? p.get("price").asInt() : product.getPrice(),
                                    p.path("display_order").asInt(product.getDisplayOrder())
                            );
                            productRepository.save(product);
                        });
                    } else {
                        // new product
                        productRepository.save(ProductEntity.builder()
                                .placeId(placeId)
                                .name(p.path("name").asText())
                                .category(p.path("category").asText(null))
                                .imageUrl(p.path("image_url").asText(null))
                                .inStock(p.path("in_stock").asBoolean(true))
                                .price(p.has("price") ? p.get("price").asInt() : null)
                                .displayOrder(p.path("display_order").asInt(0))
                                .build());
                    }
                }
            }
        } catch (JsonProcessingException e) {
            throw Status.INVALID_ARGUMENT.withDescription("Invalid proposed_changes JSON").asRuntimeException();
        }
    }
}
