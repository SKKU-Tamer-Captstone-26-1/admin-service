package com.ontheblock.admin.controller;

import com.ontheblock.admin.domain.store.entity.StoreEntity;
import com.ontheblock.admin.dto.PageResponse;
import com.ontheblock.admin.dto.StoreDto;
import com.ontheblock.admin.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Stores", description = "매장 관리 (운영자 전용)")
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @Operation(summary = "매장 목록 조회")
    @GetMapping
    public PageResponse<StoreDto.StoreResponse> listStores(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String nameSearch,
            @RequestParam(required = false) String managerUserId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        StoreEntity.StoreType storeType = type != null && !type.isBlank()
                ? StoreEntity.StoreType.valueOf(type) : null;
        StoreEntity.StoreStatus storeStatus = status != null && !status.isBlank()
                ? StoreEntity.StoreStatus.valueOf(status) : null;

        var result = storeService.listStores(storeType, storeStatus, nameSearch, managerUserId, page, pageSize);
        return PageResponse.from(result, e -> StoreDto.StoreResponse.from(e,
                storeService.getManagerIds(e.getId())));
    }

    @Operation(summary = "매장 상세 조회")
    @GetMapping("/{id}")
    public StoreDto.StoreResponse getStore(@PathVariable UUID id) {
        var store = storeService.getStore(id);
        return StoreDto.StoreResponse.from(store, storeService.getManagerIds(id));
    }

    @Operation(summary = "매장 생성 (운영자 전용)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreDto.StoreResponse createStore(
            @RequestBody StoreDto.CreateStoreRequest req, HttpServletRequest request) {
        var store = storeService.createStore(
                StoreEntity.StoreType.valueOf(req.type()),
                req.name(), req.address(), req.lat(), req.lng(),
                req.businessHours(), req.contact(),
                ActorIdResolver.resolve(request));
        return StoreDto.StoreResponse.from(store, List.of());
    }

    @Operation(summary = "매장 삭제 (소프트, 운영자 전용)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStore(@PathVariable UUID id, HttpServletRequest request) {
        storeService.deleteStore(id, ActorIdResolver.resolve(request));
    }

    @Operation(summary = "매장 제품 목록 조회")
    @GetMapping("/{id}/products")
    public List<StoreDto.ProductResponse> listProducts(@PathVariable UUID id) {
        return storeService.listProducts(id).stream()
                .map(StoreDto.ProductResponse::from)
                .toList();
    }

    @Operation(summary = "관리자 매장 배정")
    @PostMapping("/{id}/managers")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignManager(@PathVariable UUID id, @RequestBody StoreDto.AssignManagerRequest req) {
        storeService.assignManager(id, req.userId());
    }

    @Operation(summary = "관리자 매장 배정 해제")
    @DeleteMapping("/{id}/managers/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeManager(@PathVariable UUID id, @PathVariable UUID userId) {
        storeService.removeManager(id, userId);
    }
}
