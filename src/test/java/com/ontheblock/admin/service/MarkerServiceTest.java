package com.ontheblock.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontheblock.admin.domain.marker.MarkerLayerRepository;
import com.ontheblock.admin.domain.marker.MarkerPublicationEventRepository;
import com.ontheblock.admin.domain.marker.MarkerRepository;
import com.ontheblock.admin.domain.marker.entity.MarkerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkerServiceTest {

    @Mock
    private MarkerRepository markerRepository;

    @Mock
    private MarkerLayerRepository markerLayerRepository;

    @Mock
    private MarkerPublicationEventRepository publicationEventRepository;

    @Mock
    private AuditLogService auditLogService;

    private ObjectMapper objectMapper;
    private MarkerService markerService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        markerService = new MarkerService(
                markerRepository,
                markerLayerRepository,
                publicationEventRepository,
                auditLogService,
                objectMapper
        );
    }

    // Helper to build a MarkerEntity with a given filterJson
    private MarkerEntity markerWithFilterJson(UUID id, String filterJson) {
        return MarkerEntity.builder()
                .id(id)
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
     * Case 1: Existing inventory has a product item (source=product) AND a manual item;
     * updating with new manual items keeps the product item and replaces only the manual items.
     */
    @Test
    void updateMarkerInventory_preservesProductItemAndReplacesManualItems() throws Exception {
        UUID markerId = UUID.randomUUID();
        String existingFilterJson = """
                {
                  "inventory": [
                    {"name_ko": "카페라떼", "beverage_catalog_ref": "latte-001", "source": "product"},
                    {"name_ko": "구 수동메뉴", "source": "manual"}
                  ]
                }
                """;
        MarkerEntity marker = markerWithFilterJson(markerId, existingFilterJson);

        when(markerRepository.findById(markerId)).thenReturn(Optional.of(marker));
        when(markerRepository.save(any(MarkerEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        List<MarkerService.InventoryItemInput> newManualItems = List.of(
                new MarkerService.InventoryItemInput("신 수동메뉴", "manual-ref-1")
        );

        markerService.updateMarkerInventory(markerId, newManualItems);

        ArgumentCaptor<MarkerEntity> captor = ArgumentCaptor.forClass(MarkerEntity.class);
        verify(markerRepository).save(captor.capture());

        JsonNode saved = objectMapper.readTree(captor.getValue().getFilterJson());
        JsonNode inventory = saved.get("inventory");

        assertThat(inventory.isArray()).isTrue();
        assertThat(inventory.size()).isEqualTo(2);

        // First item is the product item (preserved)
        JsonNode productItem = inventory.get(0);
        assertThat(productItem.get("name_ko").asText()).isEqualTo("카페라떼");
        assertThat(productItem.get("source").asText()).isEqualTo("product");
        assertThat(productItem.get("beverage_catalog_ref").asText()).isEqualTo("latte-001");

        // Second item is the new manual item
        JsonNode manualItem = inventory.get(1);
        assertThat(manualItem.get("name_ko").asText()).isEqualTo("신 수동메뉴");
        assertThat(manualItem.get("source").asText()).isEqualTo("manual");
        assertThat(manualItem.get("beverage_catalog_ref").asText()).isEqualTo("manual-ref-1");
    }

    /**
     * Case 2: Empty/blank/null existing filter_json => result contains only the new manual items.
     */
    @Test
    void updateMarkerInventory_withNullOrBlankFilterJson_producesOnlyManualItems() throws Exception {
        UUID markerId = UUID.randomUUID();
        // Simulate null filterJson stored
        MarkerEntity marker = MarkerEntity.builder()
                .id(markerId)
                .layerCode("test-layer")
                .label("Test Marker")
                .lat(37.5)
                .lng(127.0)
                .geohash("wy7p17q")
                .placeRef(UUID.randomUUID())
                .build();
        // Override filterJson to null by using a separate entity with null
        MarkerEntity markerWithNull = MarkerEntity.builder()
                .id(markerId)
                .layerCode("test-layer")
                .label("Test Marker")
                .lat(37.5)
                .lng(127.0)
                .geohash("wy7p17q")
                .placeRef(UUID.randomUUID())
                .filterJson(null)
                .build();

        when(markerRepository.findById(markerId)).thenReturn(Optional.of(markerWithNull));
        when(markerRepository.save(any(MarkerEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        List<MarkerService.InventoryItemInput> newManualItems = List.of(
                new MarkerService.InventoryItemInput("아메리카노", null)
        );

        markerService.updateMarkerInventory(markerId, newManualItems);

        ArgumentCaptor<MarkerEntity> captor = ArgumentCaptor.forClass(MarkerEntity.class);
        verify(markerRepository).save(captor.capture());

        JsonNode saved = objectMapper.readTree(captor.getValue().getFilterJson());
        JsonNode inventory = saved.get("inventory");

        assertThat(inventory.isArray()).isTrue();
        assertThat(inventory.size()).isEqualTo(1);

        JsonNode item = inventory.get(0);
        assertThat(item.get("name_ko").asText()).isEqualTo("아메리카노");
        assertThat(item.get("source").asText()).isEqualTo("manual");
        assertThat(item.has("beverage_catalog_ref")).isFalse();
    }

    /**
     * Case 3:
     *   - Input item with blank name_ko is skipped.
     *   - Item with blank beverageCatalogRef omits that field.
     *   - source is set to "manual" on all written items.
     */
    @Test
    void updateMarkerInventory_blankNameKoSkipped_blankCatalogRefOmitted_sourceAlwaysManual() throws Exception {
        UUID markerId = UUID.randomUUID();
        MarkerEntity marker = markerWithFilterJson(markerId, "{}");

        when(markerRepository.findById(markerId)).thenReturn(Optional.of(marker));
        when(markerRepository.save(any(MarkerEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        List<MarkerService.InventoryItemInput> items = List.of(
                new MarkerService.InventoryItemInput("", "should-be-skipped"),     // blank name_ko => skip
                new MarkerService.InventoryItemInput("   ", "should-be-skipped"),  // blank/whitespace => skip
                new MarkerService.InventoryItemInput("유효한음료", ""),              // blank catalogRef => omit field
                new MarkerService.InventoryItemInput("다른음료", "ref-123")          // normal item
        );

        markerService.updateMarkerInventory(markerId, items);

        ArgumentCaptor<MarkerEntity> captor = ArgumentCaptor.forClass(MarkerEntity.class);
        verify(markerRepository).save(captor.capture());

        JsonNode saved = objectMapper.readTree(captor.getValue().getFilterJson());
        JsonNode inventory = saved.get("inventory");

        assertThat(inventory.isArray()).isTrue();
        // Only 2 items should be written (the two with blank name_ko are skipped)
        assertThat(inventory.size()).isEqualTo(2);

        JsonNode item0 = inventory.get(0);
        assertThat(item0.get("name_ko").asText()).isEqualTo("유효한음료");
        assertThat(item0.get("source").asText()).isEqualTo("manual");
        assertThat(item0.has("beverage_catalog_ref")).isFalse(); // blank => omitted

        JsonNode item1 = inventory.get(1);
        assertThat(item1.get("name_ko").asText()).isEqualTo("다른음료");
        assertThat(item1.get("source").asText()).isEqualTo("manual");
        assertThat(item1.get("beverage_catalog_ref").asText()).isEqualTo("ref-123");
    }
}
