package com.ontheblock.admin.grpc;

import com.ontheblock.admin.domain.marker.entity.MarkerLayerEntity;
import com.ontheblock.admin.grpc.mapper.ProtoMapper;
import com.ontheblock.admin.service.LayerService;
import com.ontheblock.admin.v1.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.stream.IntStream;

@GrpcService
@RequiredArgsConstructor
public class LayerAdminServiceImpl extends LayerAdminServiceGrpc.LayerAdminServiceImplBase {

    private final LayerService layerService;

    @Override
    public void listLayers(ListLayersRequest req, StreamObserver<ListLayersResponse> obs) {
        try {
            List<MarkerLayerEntity> layers = layerService.listLayers();
            ListLayersResponse.Builder resp = ListLayersResponse.newBuilder();
            layers.forEach(l -> resp.addLayers(
                    ProtoMapper.toLayerSummary(l, layerService.countMarkersForLayer(l.getCode()))));
            obs.onNext(resp.build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void createLayer(CreateLayerRequest req, StreamObserver<CreateLayerResponse> obs) {
        try {
            MarkerLayerEntity layer = layerService.createLayer(
                    req.getCode(), req.getLabelKo(), req.getLabelEn(),
                    req.getIconKey().isBlank() ? null : req.getIconKey(),
                    req.getDisplayOrder(), req.getDefaultVisible());
            obs.onNext(CreateLayerResponse.newBuilder()
                    .setLayer(ProtoMapper.toLayerSummary(layer, 0))
                    .build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void updateLayer(UpdateLayerRequest req, StreamObserver<UpdateLayerResponse> obs) {
        try {
            MarkerLayerEntity layer = layerService.updateLayer(
                    req.getCode(), req.getLabelKo(), req.getLabelEn(),
                    req.getIconKey().isBlank() ? null : req.getIconKey(),
                    req.getDefaultVisible(), req.getIsActive());
            int count = layerService.countMarkersForLayer(req.getCode());
            obs.onNext(UpdateLayerResponse.newBuilder()
                    .setLayer(ProtoMapper.toLayerSummary(layer, count))
                    .build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void deleteLayer(DeleteLayerRequest req, StreamObserver<DeleteLayerResponse> obs) {
        try {
            layerService.deleteLayer(req.getCode());
            obs.onNext(DeleteLayerResponse.newBuilder().build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void reorderLayers(ReorderLayersRequest req, StreamObserver<ReorderLayersResponse> obs) {
        try {
            List<String> codes  = req.getEntriesList().stream().map(LayerOrderEntry::getCode).toList();
            List<Integer> orders = req.getEntriesList().stream().map(LayerOrderEntry::getDisplayOrder).toList();
            layerService.reorderLayers(codes, orders);
            obs.onNext(ReorderLayersResponse.newBuilder().build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }
}
