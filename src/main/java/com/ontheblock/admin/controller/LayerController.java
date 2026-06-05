package com.ontheblock.admin.controller;

import com.ontheblock.admin.dto.LayerDto;
import com.ontheblock.admin.service.LayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Layers", description = "마커 레이어 관리 (운영자 전용)")
@RestController
@RequestMapping("/api/layers")
@RequiredArgsConstructor
public class LayerController {

    private final LayerService layerService;

    @Operation(summary = "레이어 목록 조회 (display_order 기준 정렬)")
    @GetMapping
    public List<LayerDto.LayerResponse> listLayers() {
        return layerService.listLayers().stream()
                .map(l -> LayerDto.LayerResponse.from(l, layerService.countMarkersForLayer(l.getCode())))
                .toList();
    }

    @Operation(summary = "레이어 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LayerDto.LayerResponse createLayer(@RequestBody LayerDto.CreateLayerRequest req) {
        return LayerDto.LayerResponse.from(
                layerService.createLayer(req.code(), req.labelKo(), req.labelEn(),
                        req.iconKey(), req.displayOrder(), req.defaultVisible()), 0);
    }

    @Operation(summary = "레이어 수정")
    @PatchMapping("/{code}")
    public LayerDto.LayerResponse updateLayer(
            @PathVariable String code, @RequestBody LayerDto.UpdateLayerRequest req) {
        var layer = layerService.updateLayer(code, req.labelKo(), req.labelEn(),
                req.iconKey(), req.defaultVisible(), req.isActive());
        return LayerDto.LayerResponse.from(layer, layerService.countMarkersForLayer(code));
    }

    @Operation(summary = "레이어 삭제 (마커 존재 시 400 반환)")
    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLayer(@PathVariable String code) {
        layerService.deleteLayer(code);
    }

    @Operation(summary = "레이어 순서 재정렬")
    @PutMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderLayers(@RequestBody LayerDto.ReorderRequest req) {
        var codes  = req.entries().stream().map(LayerDto.LayerOrderEntry::code).toList();
        var orders = req.entries().stream().map(LayerDto.LayerOrderEntry::displayOrder).toList();
        layerService.reorderLayers(codes, orders);
    }
}
