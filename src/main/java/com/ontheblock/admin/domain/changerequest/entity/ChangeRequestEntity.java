package com.ontheblock.admin.domain.changerequest.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "change_requests", schema = "admin")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChangeRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private TargetType targetType;

    @Column(name = "target_ref", nullable = false)
    private UUID targetRef;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "proposed_changes", nullable = false, columnDefinition = "jsonb")
    private String proposedChanges;

    @Column(name = "attachments", columnDefinition = "TEXT[]")
    @Builder.Default
    private String[] attachments = new String[0];

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "reviewer_id")
    private UUID reviewerId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_comment")
    private String reviewComment;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public void approve(UUID reviewerId) {
        this.status = Status.APPROVED;
        this.reviewerId = reviewerId;
        this.reviewedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void reject(UUID reviewerId, String reviewComment) {
        this.status = Status.REJECTED;
        this.reviewerId = reviewerId;
        this.reviewedAt = LocalDateTime.now();
        this.reviewComment = reviewComment;
        this.updatedAt = LocalDateTime.now();
    }

    public void withdraw() {
        this.status = Status.WITHDRAWN;
        this.updatedAt = LocalDateTime.now();
    }

    public enum TargetType {
        STORE_INFO, PRODUCT_LIST, PRODUCT_IMAGE
    }

    public enum Status {
        PENDING, APPROVED, REJECTED, WITHDRAWN
    }
}
