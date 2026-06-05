package com.ontheblock.admin.service;

import com.ontheblock.admin.domain.changerequest.ChangeRequestRepository;
import com.ontheblock.admin.domain.changerequest.entity.ChangeRequestEntity;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChangeRequestService {

    private final ChangeRequestRepository changeRequestRepository;
    private final StoreService storeService;
    private final MarkerService markerService;
    private final AuditLogService auditLogService;

    @Transactional
    public ChangeRequestEntity submit(UUID requesterId, ChangeRequestEntity.TargetType targetType,
                                       UUID targetRef, String proposedChanges, String[] attachments) {
        // verify requester is a manager of the target store
        if (!storeService.isManagerOfStore(requesterId, targetRef)) {
            throw Status.PERMISSION_DENIED
                    .withDescription("User is not a manager of the target store").asRuntimeException();
        }
        return changeRequestRepository.save(ChangeRequestEntity.builder()
                .requesterId(requesterId)
                .targetType(targetType)
                .targetRef(targetRef)
                .proposedChanges(proposedChanges)
                .attachments(attachments != null ? attachments : new String[0])
                .build());
    }

    @Transactional(readOnly = true)
    public Page<ChangeRequestEntity> listMyRequests(UUID requesterId,
                                                     ChangeRequestEntity.Status status,
                                                     int page, int pageSize) {
        if (status != null) {
            return changeRequestRepository.findAllWithFilters(
                    status, requesterId, null, null, PageRequest.of(page - 1, pageSize));
        }
        return changeRequestRepository.findAllByRequesterIdOrderByCreatedAtDesc(
                requesterId, PageRequest.of(page - 1, pageSize));
    }

    @Transactional
    public ChangeRequestEntity withdraw(UUID id, UUID requesterId) {
        ChangeRequestEntity cr = getById(id);
        if (!cr.getRequesterId().equals(requesterId)) {
            throw Status.PERMISSION_DENIED.withDescription("Not the owner of this request").asRuntimeException();
        }
        if (cr.getStatus() != ChangeRequestEntity.Status.PENDING) {
            throw Status.FAILED_PRECONDITION
                    .withDescription("Only PENDING requests can be withdrawn").asRuntimeException();
        }
        cr.withdraw();
        return changeRequestRepository.save(cr);
    }

    @Transactional(readOnly = true)
    public Page<ChangeRequestEntity> listAll(ChangeRequestEntity.Status status, UUID requesterId,
                                              LocalDateTime from, LocalDateTime to,
                                              int page, int pageSize) {
        return changeRequestRepository.findAllWithFilters(
                status, requesterId, from, to, PageRequest.of(page - 1, pageSize));
    }

    @Transactional(readOnly = true)
    public ChangeRequestEntity getById(UUID id) {
        return changeRequestRepository.findById(id)
                .orElseThrow(() -> Status.NOT_FOUND.withDescription("Change request not found").asRuntimeException());
    }

    @Transactional
    public ChangeRequestEntity approve(UUID id, UUID reviewerId, boolean autoRepublishMarker) {
        ChangeRequestEntity cr = getById(id);
        if (cr.getStatus() != ChangeRequestEntity.Status.PENDING) {
            throw Status.FAILED_PRECONDITION
                    .withDescription("Only PENDING requests can be approved").asRuntimeException();
        }

        applyChanges(cr);

        if (autoRepublishMarker) {
            markerService.publishMarkersByPlaceRef(cr.getTargetRef(), reviewerId);
        }

        cr.approve(reviewerId);
        ChangeRequestEntity saved = changeRequestRepository.save(cr);
        auditLogService.log(reviewerId, "APPROVE_CHANGE_REQUEST", "CHANGE_REQUEST", id, null);
        return saved;
    }

    @Transactional
    public ChangeRequestEntity reject(UUID id, UUID reviewerId, String reviewComment) {
        if (reviewComment == null || reviewComment.isBlank()) {
            throw Status.INVALID_ARGUMENT.withDescription("review_comment is required for rejection").asRuntimeException();
        }
        ChangeRequestEntity cr = getById(id);
        if (cr.getStatus() != ChangeRequestEntity.Status.PENDING) {
            throw Status.FAILED_PRECONDITION
                    .withDescription("Only PENDING requests can be rejected").asRuntimeException();
        }
        cr.reject(reviewerId, reviewComment);
        ChangeRequestEntity saved = changeRequestRepository.save(cr);
        auditLogService.log(reviewerId, "REJECT_CHANGE_REQUEST", "CHANGE_REQUEST", id,
                "{\"comment\":\"" + reviewComment + "\"}");
        return saved;
    }

    private void applyChanges(ChangeRequestEntity cr) {
        switch (cr.getTargetType()) {
            case STORE_INFO -> storeService.applyStoreInfoChanges(cr.getTargetRef(), cr.getProposedChanges());
            case PRODUCT_LIST -> storeService.applyProductListChanges(cr.getTargetRef(), cr.getProposedChanges());
            case PRODUCT_IMAGE -> {
                // image_url is already set in proposed_changes; apply via PRODUCT_LIST path
                storeService.applyProductListChanges(cr.getTargetRef(), cr.getProposedChanges());
            }
        }
    }

    public int countPending() {
        return changeRequestRepository.countByStatus(ChangeRequestEntity.Status.PENDING);
    }
}
