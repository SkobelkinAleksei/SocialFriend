package org.example.user.entity.admin;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "admin_reports", indexes = {
        @Index(name = "idx_admin_reports_status", columnList = "status"),
        @Index(name = "idx_admin_reports_accused", columnList = "accused_id"),
        @Index(name = "idx_admin_reports_category", columnList = "category")
})
public class AdminReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "accused_id", nullable = false)
    private Long accusedId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private AdminReportCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 20)
    private AdminReportReason reason;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "target_title", length = 250)
    private String targetTitle;

    @Column(name = "snapshot_text", columnDefinition = "TEXT")
    private String snapshotText;

    @Column(name = "details", length = 500)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AdminReportStatus status = AdminReportStatus.OPEN;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at", columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewer_id")
    private Long reviewerId;
}
