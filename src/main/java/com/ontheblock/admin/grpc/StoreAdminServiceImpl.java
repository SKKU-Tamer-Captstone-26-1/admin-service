package com.ontheblock.admin.grpc;

import com.ontheblock.admin.domain.store.entity.StoreEntity;
import com.ontheblock.admin.grpc.mapper.ProtoMapper;
import com.ontheblock.admin.service.ProductMediaStorageService;
import com.ontheblock.admin.service.StoreService;
import com.ontheblock.admin.v1.*;
import com.ontheblock.common.v1.PaginationResponse;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class StoreAdminServiceImpl extends StoreAdminServiceGrpc.StoreAdminServiceImplBase {

    private final StoreService storeService;
    private final ProductMediaStorageService productMediaStorageService;

    @Override
    public void listStores(ListStoresRequest req, StreamObserver<ListStoresResponse> obs) {
        try {
            int page = req.getPagination().getPage() > 0 ? req.getPagination().getPage() : 1;
            int pageSize = req.getPagination().getPageSize() > 0 ? req.getPagination().getPageSize() : 20;

            StoreEntity.StoreType type = req.getTypeFilter() == StoreType.STORE_TYPE_UNSPECIFIED
                    ? null : ProtoMapper.toStoreTypeEntity(req.getTypeFilter());
            StoreEntity.StoreStatus status = req.getStatusFilter() == StoreStatus.STORE_STATUS_UNSPECIFIED
                    ? null : ProtoMapper.toStoreStatusEntity(req.getStatusFilter());

            Page<StoreEntity> result = storeService.listStores(
                    type, status,
                    req.getNameSearch().isBlank() ? null : req.getNameSearch(),
                    req.getManagerUserId().isBlank() ? null : req.getManagerUserId(),
                    page, pageSize);

            ListStoresResponse.Builder resp = ListStoresResponse.newBuilder()
                    .setPagination(PaginationResponse.newBuilder()
                            .setTotalCount((int) result.getTotalElements())
                            .setPage(page).setPageSize(pageSize).setHasNext(result.hasNext()).build());
            result.getContent().forEach(s -> {
                List<String> managerIds = storeService.getManagerIds(s.getId()).stream()
                        .map(UUID::toString).toList();
                resp.addStores(ProtoMapper.toStoreSummary(s, managerIds));
            });
            obs.onNext(resp.build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void getStore(GetStoreRequest req, StreamObserver<GetStoreResponse> obs) {
        try {
            UUID id = UUID.fromString(req.getId());
            StoreEntity store = storeService.getStore(id);
            List<String> managerIds = storeService.getManagerIds(id).stream()
                    .map(UUID::toString).toList();
            List<ProductSummary> products = storeService.listProducts(id).stream()
                    .map(ProtoMapper::toProductSummary).toList();
            obs.onNext(GetStoreResponse.newBuilder()
                    .setStore(ProtoMapper.toStoreDetail(store, managerIds))
                    .addAllProducts(products)
                    .build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void createStore(CreateStoreRequest req, StreamObserver<CreateStoreResponse> obs) {
        try {
            UUID actorId = GrpcContextUtil.getUserId();
            StoreEntity store = storeService.createStore(
                    ProtoMapper.toStoreTypeEntity(req.getType()),
                    req.getName(), req.getAddress(), req.getLat(), req.getLng(),
                    req.getBusinessHours().isEmpty() ? null : req.getBusinessHours().toStringUtf8(),
                    req.getContact().isBlank() ? null : req.getContact(),
                    actorId);
            obs.onNext(CreateStoreResponse.newBuilder()
                    .setStore(ProtoMapper.toStoreDetail(store, List.of()))
                    .build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void deleteStore(DeleteStoreRequest req, StreamObserver<DeleteStoreResponse> obs) {
        try {
            UUID actorId = GrpcContextUtil.getUserId();
            storeService.deleteStore(UUID.fromString(req.getId()), actorId);
            obs.onNext(DeleteStoreResponse.newBuilder().build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void listProducts(ListProductsRequest req, StreamObserver<ListProductsResponse> obs) {
        try {
            ListProductsResponse.Builder resp = ListProductsResponse.newBuilder();
            storeService.listProducts(UUID.fromString(req.getPlaceId()))
                    .forEach(p -> resp.addProducts(ProtoMapper.toProductSummary(p)));
            obs.onNext(resp.build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void assignManagerToStore(AssignManagerToStoreRequest req,
                                      StreamObserver<AssignManagerToStoreResponse> obs) {
        try {
            storeService.assignManager(
                    UUID.fromString(req.getPlaceId()),
                    UUID.fromString(req.getUserId()));
            obs.onNext(AssignManagerToStoreResponse.newBuilder().build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void removeManagerFromStore(RemoveManagerFromStoreRequest req,
                                        StreamObserver<RemoveManagerFromStoreResponse> obs) {
        try {
            storeService.removeManager(
                    UUID.fromString(req.getPlaceId()),
                    UUID.fromString(req.getUserId()));
            obs.onNext(RemoveManagerFromStoreResponse.newBuilder().build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void generateProductImageUploadUrl(GenerateProductImageUploadUrlRequest req,
                                               StreamObserver<GenerateProductImageUploadUrlResponse> obs) {
        try {
            // Validate the linked place exists (reuses Status.NOT_FOUND semantics from StoreService).
            storeService.getStore(UUID.fromString(req.getPlaceId()));

            ProductMediaStorageService.UploadUrlResult result =
                    productMediaStorageService.generateUploadUrl(req.getProductId());

            obs.onNext(GenerateProductImageUploadUrlResponse.newBuilder()
                    .setUploadUrl(result.uploadUrl())
                    .setObjectUrl(result.objectUrl())
                    .build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }
}
