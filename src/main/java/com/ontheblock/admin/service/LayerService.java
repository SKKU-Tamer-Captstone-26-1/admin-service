package com.ontheblock.admin.service;

import com.ontheblock.admin.domain.marker.MarkerLayerRepository;
import com.ontheblock.admin.domain.marker.MarkerRepository;
import com.ontheblock.admin.domain.marker.entity.MarkerLayerEntity;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LayerService {

    private final MarkerLayerRepository layerRepository;
    private final MarkerRepository markerRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<MarkerLayerEntity> listLayers() {
        return layerRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public MarkerLayerEntity getLayer(String code) {
        return layerRepository.findById(code)
                .orElseThrow(() -> Status.NOT_FOUND.withDescription("Layer not found: " + code).asRuntimeException());
    }

    @Transactional
    public MarkerLayerEntity createLayer(String code, String labelKo, String labelEn,
                                         String iconKey, int displayOrder, boolean defaultVisible) {
        if (layerRepository.existsById(code)) {
            throw Status.ALREADY_EXISTS.withDescription("Layer code already exists: " + code).asRuntimeException();
        }
        return layerRepository.save(MarkerLayerEntity.builder()
                .code(code)
                .labelKo(labelKo)
                .labelEn(labelEn)
                .iconKey(iconKey)
                .displayOrder(displayOrder)
                .defaultVisible(defaultVisible)
                .build());
    }

    @Transactional
    public MarkerLayerEntity updateLayer(String code, String labelKo, String labelEn,
                                          String iconKey, boolean defaultVisible, boolean isActive) {
        MarkerLayerEntity layer = getLayer(code);
        layer.update(labelKo, labelEn, iconKey, defaultVisible, isActive);
        return layerRepository.save(layer);
    }

    @Transactional
    public void deleteLayer(String code) {
        int markerCount = markerRepository.countByLayerCode(code);
        if (markerCount > 0) {
            throw Status.FAILED_PRECONDITION
                    .withDescription("Cannot delete layer '" + code + "': " + markerCount + " marker(s) still use it")
                    .asRuntimeException();
        }
        layerRepository.deleteById(code);
    }

    @Transactional
    public void reorderLayers(List<String> codes, List<Integer> orders) {
        for (int i = 0; i < codes.size(); i++) {
            MarkerLayerEntity layer = getLayer(codes.get(i));
            layer.updateDisplayOrder(orders.get(i));
            layerRepository.save(layer);
        }
    }

    @Transactional(readOnly = true)
    public int countMarkersForLayer(String code) {
        return markerRepository.countByLayerCode(code);
    }
}
