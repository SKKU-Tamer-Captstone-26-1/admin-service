package com.ontheblock.admin.grpc;

import com.ontheblock.admin.domain.changerequest.entity.ChangeRequestEntity;
import com.ontheblock.admin.grpc.mapper.ProtoMapper;
import com.ontheblock.admin.service.ChangeRequestService;
import com.ontheblock.admin.service.StoreService;
import com.ontheblock.admin.v1.*;
import com.ontheblock.common.v1.PaginationResponse;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class ChangeRequestServiceImpl extends ChangeRequestServiceGrpc.ChangeRequestServiceImplBase {

    private final ChangeRequestService changeRequestService;
    private final StoreService storeService;

    @Override
    public void submitChangeRequest(SubmitChangeRequestRequest req,
                                     StreamObserver<SubmitChangeRequestResponse> obs) {
        try {
            UUID requesterId = GrpcContextUtil.getUserId();
            ChangeRequestEntity cr = changeRequestService.submit(
                    requesterId,
                    ProtoMapper.toTargetTypeEntity(req.getTargetType()),
                    UUID.fromString(req.getTargetRef()),
                    req.getProposedChanges().toStringUtf8(),
                    req.getAttachmentUrlsList().toArray(new String[0]));
            obs.onNext(SubmitChangeRequestResponse.newBuilder()
                    .setChangeRequest(ProtoMapper.toChangeRequestSummary(cr))
                    .build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void listMyChangeRequests(ListMyChangeRequestsRequest req,
                                      StreamObserver<ListMyChangeRequestsResponse> obs) {
        try {
            UUID requesterId = GrpcContextUtil.getUserId();
            int page = req.getPagination().getPage() > 0 ? req.getPagination().getPage() : 1;
            int pageSize = req.getPagination().getPageSize() > 0 ? req.getPagination().getPageSize() : 20;

            ChangeRequestEntity.Status status = req.getStatusFilter() == ChangeRequestStatus.CHANGE_REQUEST_STATUS_UNSPECIFIED
                    ? null : ProtoMapper.toStatusEntity(req.getStatusFilter());

            Page<ChangeRequestEntity> result = changeRequestService.listMyRequests(
                    requesterId, status, page, pageSize);

            ListMyChangeRequestsResponse.Builder resp = ListMyChangeRequestsResponse.newBuilder()
                    .setPagination(PaginationResponse.newBuilder()
                            .setTotalCount((int) result.getTotalElements())
                            .setPage(page).setPageSize(pageSize).setHasNext(result.hasNext()).build());
            result.getContent().forEach(cr -> resp.addChangeRequests(ProtoMapper.toChangeRequestSummary(cr)));
            obs.onNext(resp.build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void withdrawChangeRequest(WithdrawChangeRequestRequest req,
                                       StreamObserver<WithdrawChangeRequestResponse> obs) {
        try {
            UUID requesterId = GrpcContextUtil.getUserId();
            ChangeRequestEntity cr = changeRequestService.withdraw(
                    UUID.fromString(req.getId()), requesterId);
            obs.onNext(WithdrawChangeRequestResponse.newBuilder()
                    .setChangeRequest(ProtoMapper.toChangeRequestSummary(cr))
                    .build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void listChangeRequests(ListChangeRequestsRequest req,
                                    StreamObserver<ListChangeRequestsResponse> obs) {
        try {
            int page = req.getPagination().getPage() > 0 ? req.getPagination().getPage() : 1;
            int pageSize = req.getPagination().getPageSize() > 0 ? req.getPagination().getPageSize() : 20;

            ChangeRequestEntity.Status status = req.getStatusFilter() == ChangeRequestStatus.CHANGE_REQUEST_STATUS_UNSPECIFIED
                    ? null : ProtoMapper.toStatusEntity(req.getStatusFilter());
            UUID requesterId = req.getRequesterId().isBlank() ? null : UUID.fromString(req.getRequesterId());
            LocalDateTime from = req.hasFrom() ? LocalDateTime.ofEpochSecond(req.getFrom().getSeconds(), 0, java.time.ZoneOffset.UTC) : null;
            LocalDateTime to   = req.hasTo()   ? LocalDateTime.ofEpochSecond(req.getTo().getSeconds(),   0, java.time.ZoneOffset.UTC) : null;

            Page<ChangeRequestEntity> result = changeRequestService.listAll(
                    status, requesterId, from, to, page, pageSize);

            ListChangeRequestsResponse.Builder resp = ListChangeRequestsResponse.newBuilder()
                    .setPagination(PaginationResponse.newBuilder()
                            .setTotalCount((int) result.getTotalElements())
                            .setPage(page).setPageSize(pageSize).setHasNext(result.hasNext()).build());
            result.getContent().forEach(cr -> resp.addChangeRequests(ProtoMapper.toChangeRequestSummary(cr)));
            obs.onNext(resp.build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void getChangeRequest(GetChangeRequestRequest req,
                                  StreamObserver<GetChangeRequestResponse> obs) {
        try {
            ChangeRequestEntity cr = changeRequestService.getById(UUID.fromString(req.getId()));
            // build current snapshot for diff view
            String snapshot = buildCurrentSnapshot(cr);
            obs.onNext(GetChangeRequestResponse.newBuilder()
                    .setChangeRequest(ProtoMapper.toChangeRequestDetail(cr, snapshot))
                    .build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void approveChangeRequest(ApproveChangeRequestRequest req,
                                      StreamObserver<ApproveChangeRequestResponse> obs) {
        try {
            UUID reviewerId = GrpcContextUtil.getUserId();
            ChangeRequestEntity cr = changeRequestService.approve(
                    UUID.fromString(req.getId()), reviewerId, req.getAutoRepublishMarker());
            obs.onNext(ApproveChangeRequestResponse.newBuilder()
                    .setChangeRequest(ProtoMapper.toChangeRequestSummary(cr))
                    .build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    @Override
    public void rejectChangeRequest(RejectChangeRequestRequest req,
                                     StreamObserver<RejectChangeRequestResponse> obs) {
        try {
            UUID reviewerId = GrpcContextUtil.getUserId();
            ChangeRequestEntity cr = changeRequestService.reject(
                    UUID.fromString(req.getId()), reviewerId, req.getReviewComment());
            obs.onNext(RejectChangeRequestResponse.newBuilder()
                    .setChangeRequest(ProtoMapper.toChangeRequestSummary(cr))
                    .build());
            obs.onCompleted();
        } catch (Exception e) {
            obs.onError(e);
        }
    }

    private String buildCurrentSnapshot(ChangeRequestEntity cr) {
        try {
            var store = storeService.getStore(cr.getTargetRef());
            return "{\"name\":\"" + store.getName() + "\",\"address\":\"" + store.getAddress() + "\"}";
        } catch (Exception e) {
            return "{}";
        }
    }
}
