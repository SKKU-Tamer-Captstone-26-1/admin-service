package com.ontheblock.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontheblock.admin.domain.marker.MarkerRepository;
import com.ontheblock.admin.domain.marker.entity.MarkerEntity;
import com.ontheblock.admin.domain.store.ProductRepository;
import com.ontheblock.admin.domain.store.StoreManagerMappingRepository;
import com.ontheblock.admin.domain.store.StoreRepository;
import com.ontheblock.admin.domain.store.entity.ProductEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StoreManagerMappingRepository mappingRepository;

    @Mock
    private MarkerRepository markerRepository;

    @Mock
    private AuditLogService auditLogService;

    private ObjectMapper objectMapper;
    private StoreService storeService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        storeService = new StoreService(
                storeRepository,
                productRepository,
                mappingRepository,
                markerRepository,
                auditLogService,
                objectMapper
        );
    }

    // Helper to build a ProductEntity
    private ProductEntity product(String name, String beverageCatalogRef) {
        return ProductEntity.builder()
                .id(UUID.randomUUID())
                .placeId(UUID.randomUUID())
                .name(name)
                .beverageCatalogRef(beverageCatalogRef)
                .build();
    }

    // Helper to build a MarkerEntity with a given filterJson
    private MarkerEntity markerWithFilterJson(String filterJson) {
        return MarkerEntity.builder()
                .id(UUID.randomUUID())
                .layerCode("test-layer")
                .label("Test Marker")
                .lat(37.5)
                .lng(127.0)
                .geohash("wy7p17q")
                .placeRef(UUID.randomUUID())
                .filterJson(filterJson)
                .build();
    }

    /**
     * Case 4: Products present + existing marker filter_json with a manual item =>
     * merged result has product items (source=product, from products) AND preserves the manual item.
     */
    @Test
    void syncMarkerInventoryFromProducts_mergesProductItemsAndPreservesManualItem() throws Exception {
        UUID placeId = UUID.randomUUID();

        List<ProductEntity> products = List.of(
                product("카페라떼", "latte-001"),
                product("아메리카노", "americano-002")
        );

        String existingFilterJson = """
                {
                  "inventory": [
                    {"name_ko": "수동추가음료", "source": "manual"}
                  ]
                }
                """;
        MarkerEntity marker = markerWithFilterJson(existingFilterJson);

        when(productRepository.findAllByPlaceIdAndDeletedAtIsNullOrderByDisplayOrderAsc(placeId))
                .thenReturn(products);
        when(markerRepository.findAllByPlaceRefAndDeletedAtIsNull(placeId))
                .thenReturn(List.of(marker));
        when(markerRepository.save(any(MarkerEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        storeService.syncMarkerInventoryFromProducts(placeId);

        ArgumentCaptor<MarkerEntity> captor = ArgumentCaptor.forClass(MarkerEntity.class);
        verify(markerRepository).save(captor.capture());

        JsonNode saved = objectMapper.readTree(captor.getValue().getFilterJson());
        JsonNode inventory = saved.get("inventory");

        assertThat(inventory.isArray()).isTrue();
        // 2 product items + 1 manual item
        assertThat(inventory.size()).isEqualTo(3);

        // Product items first
        JsonNode p0 = inventory.get(0);
        assertThat(p0.get("name_ko").asText()).isEqualTo("카페라떼");
        assertThat(p0.get("source").asText()).isEqualTo("product");
        assertThat(p0.get("beverage_catalog_ref").asText()).isEqualTo("latte-001");

        JsonNode p1 = inventory.get(1);
        assertThat(p1.get("name_ko").asText()).isEqualTo("아메리카노");
        assertThat(p1.get("source").asText()).isEqualTo("product");
        assertThat(p1.get("beverage_catalog_ref").asText()).isEqualTo("americano-002");

        // Manual item preserved at the end
        JsonNode manualItem = inventory.get(2);
        assertThat(manualItem.get("name_ko").asText()).isEqualTo("수동추가음료");
        assertThat(manualItem.get("source").asText()).isEqualTo("manual");
    }

    /**
     * Case 5: Legacy untagged existing item (no source field) is dropped after sync.
     */
    @Test
    void syncMarkerInventoryFromProducts_dropsLegacyUntaggedItems() throws Exception {
        UUID placeId = UUID.randomUUID();

        List<ProductEntity> products = List.of(
                product("신메뉴", "new-ref-001")
        );

        String existingFilterJson = """
                {
                  "inventory": [
                    {"name_ko": "레거시아이템"},
                    {"name_ko": "수동아이템", "source": "manual"}
                  ]
                }
                """;
        MarkerEntity marker = markerWithFilterJson(existingFilterJson);

        when(productRepository.findAllByPlaceIdAndDeletedAtIsNullOrderByDisplayOrderAsc(placeId))
                .thenReturn(products);
        when(markerRepository.findAllByPlaceRefAndDeletedAtIsNull(placeId))
                .thenReturn(List.of(marker));
        when(markerRepository.save(any(MarkerEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        storeService.syncMarkerInventoryFromProducts(placeId);

        ArgumentCaptor<MarkerEntity> captor = ArgumentCaptor.forClass(MarkerEntity.class);
        verify(markerRepository).save(captor.capture());

        JsonNode saved = objectMapper.readTree(captor.getValue().getFilterJson());
        JsonNode inventory = saved.get("inventory");

        assertThat(inventory.isArray()).isTrue();
        // 1 product item + 1 manual item; legacy untagged item dropped
        assertThat(inventory.size()).isEqualTo(2);

        // Verify legacy item not present
        for (JsonNode item : inventory) {
            assertThat(item.get("name_ko").asText()).isNotEqualTo("레거시아이템");
        }

        // Product item present
        JsonNode productItem = inventory.get(0);
        assertThat(productItem.get("source").asText()).isEqualTo("product");
        assertThat(productItem.get("name_ko").asText()).isEqualTo("신메뉴");

        // Manual item preserved
        JsonNode manualItem = inventory.get(1);
        assertThat(manualItem.get("source").asText()).isEqualTo("manual");
        assertThat(manualItem.get("name_ko").asText()).isEqualTo("수동아이템");
    }

    /**
     * Case 6: No markers for the place => repository save is never called.
     */
    @Test
    void syncMarkerInventoryFromProducts_noMarkers_saveNeverCalled() {
        UUID placeId = UUID.randomUUID();

        when(productRepository.findAllByPlaceIdAndDeletedAtIsNullOrderByDisplayOrderAsc(placeId))
                .thenReturn(List.of(product("아무음료", "ref-x")));
        when(markerRepository.findAllByPlaceRefAndDeletedAtIsNull(placeId))
                .thenReturn(List.of());

        storeService.syncMarkerInventoryFromProducts(placeId);

        verify(markerRepository, never()).save(any());
    }

    /**
     * Case 7: A product with blank/null beverageCatalogRef => its written item omits
     * beverage_catalog_ref but still has name_ko + source=product.
     */
    @Test
    void syncMarkerInventoryFromProducts_productWithNullCatalogRef_omitsCatalogRefField() throws Exception {
        UUID placeId = UUID.randomUUID();

        List<ProductEntity> products = List.of(
                product("카탈로그없는음료", null),
                product("카탈로그빈문자", "")
        );

        MarkerEntity marker = markerWithFilterJson("{}");

        when(productRepository.findAllByPlaceIdAndDeletedAtIsNullOrderByDisplayOrderAsc(placeId))
                .thenReturn(products);
        when(markerRepository.findAllByPlaceRefAndDeletedAtIsNull(placeId))
                .thenReturn(List.of(marker));
        when(markerRepository.save(any(MarkerEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        storeService.syncMarkerInventoryFromProducts(placeId);

        ArgumentCaptor<MarkerEntity> captor = ArgumentCaptor.forClass(MarkerEntity.class);
        verify(markerRepository).save(captor.capture());

        JsonNode saved = objectMapper.readTree(captor.getValue().getFilterJson());
        JsonNode inventory = saved.get("inventory");

        assertThat(inventory.isArray()).isTrue();
        assertThat(inventory.size()).isEqualTo(2);

        JsonNode item0 = inventory.get(0);
        assertThat(item0.get("name_ko").asText()).isEqualTo("카탈로그없는음료");
        assertThat(item0.get("source").asText()).isEqualTo("product");
        assertThat(item0.has("beverage_catalog_ref")).isFalse(); // null => omitted

        JsonNode item1 = inventory.get(1);
        assertThat(item1.get("name_ko").asText()).isEqualTo("카탈로그빈문자");
        assertThat(item1.get("source").asText()).isEqualTo("product");
        assertThat(item1.has("beverage_catalog_ref")).isFalse(); // blank => omitted
    }
}
