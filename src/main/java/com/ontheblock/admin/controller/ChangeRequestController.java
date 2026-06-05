package com.ontheblock.admin.controller;

import com.ontheblock.admin.domain.changerequest.entity.ChangeRequestEntity;
import com.ontheblock.admin.dto.ChangeRequestDto;
import com.ontheblock.admin.dto.PageResponse;
import com.ontheblock.admin.service.ChangeRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Change Requests", description = "매장 수정 요청 워크플로우")
@RestController
@RequestMapping("/api/change-requests")
@RequiredArgsConstructor
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;

    // ---- 운영자 측 ----

    @Operation(summary = "[운영자] 전체 수정 요청 목록")
    @GetMapping
    public PageResponse<ChangeRequestDto.ChangeRequestResponse> listAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID requesterId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        ChangeRequestEntity.Status s = status != null && !status.isBlank()
                ? ChangeRequestEntity.Status.valueOf(status) : null;
        Page<ChangeRequestEntity> result = changeRequestService.listAll(
                s, requesterId, null, null, page, pageSize);
        return PageResponse.from(result, ChangeRequestDto.ChangeRequestResponse::from);
    }

    @Operation(summary = "[운영자] 수정 요청 상세")
    @GetMapping("/{id}")
    public ChangeRequestDto.ChangeRequestResponse get(@PathVariable UUID id) {
        return ChangeRequestDto.ChangeRequestResponse.from(changeRequestService.getById(id));
    }

    @Operation(summary = "[운영자] 요청 승인 (auto_republish_marker=true 시 마커 자동 재발행)")
    @PostMapping("/{id}/approve")
    public ChangeRequestDto.ChangeRequestResponse approve(
            @PathVariable UUID id,
            @RequestBody ChangeRequestDto.ApproveRequest req,
            HttpServletRequest request) {
        return ChangeRequestDto.ChangeRequestResponse.from(
                changeRequestService.approve(id, ActorIdResolver.resolve(request), req.autoRepublishMarker()));
    }

    @Operation(summary = "[운영자] 요청 거부 (review_comment 필수)")
    @PostMapping("/{id}/reject")
    public ChangeRequestDto.ChangeRequestResponse reject(
            @PathVariable UUID id,
            @RequestBody ChangeRequestDto.RejectRequest req,
            HttpServletRequest request) {
        return ChangeRequestDto.ChangeRequestResponse.from(
                changeRequestService.reject(id, ActorIdResolver.resolve(request), req.reviewComment()));
    }

    // ---- 관리자 측 ----

    @Operation(summary = "[관리자] 수정 요청 제출 (X-User-Id 헤더 = 요청자 user_id)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChangeRequestDto.ChangeRequestResponse submit(
            @RequestBody ChangeRequestDto.SubmitRequest req,
            HttpServletRequest request) {
        return ChangeRequestDto.ChangeRequestResponse.from(
                changeRequestService.submit(
                        ActorIdResolver.resolve(request),
                        ChangeRequestEntity.TargetType.valueOf(req.targetType()),
                        req.targetRef(),
                        req.proposedChanges(),
                        req.attachmentUrls() != null ? req.attachmentUrls().toArray(new String[0]) : new String[0]));
    }

    @Operation(summary = "[관리자] 내 요청 목록 (X-User-Id 헤더 = 요청자 user_id)")
    @GetMapping("/my")
    public PageResponse<ChangeRequestDto.ChangeRequestResponse> listMy(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        UUID requesterId = ActorIdResolver.resolve(request);
        ChangeRequestEntity.Status s = status != null && !status.isBlank()
                ? ChangeRequestEntity.Status.valueOf(status) : null;
        Page<ChangeRequestEntity> result = changeRequestService.listMyRequests(requesterId, s, page, pageSize);
        return PageResponse.from(result, ChangeRequestDto.ChangeRequestResponse::from);
    }

    @Operation(summary = "[관리자] 요청 철회 (PENDING 상태에서만 가능)")
    @DeleteMapping("/{id}/withdraw")
    public ChangeRequestDto.ChangeRequestResponse withdraw(
            @PathVariable UUID id, HttpServletRequest request) {
        return ChangeRequestDto.ChangeRequestResponse.from(
                changeRequestService.withdraw(id, ActorIdResolver.resolve(request)));
    }
}
