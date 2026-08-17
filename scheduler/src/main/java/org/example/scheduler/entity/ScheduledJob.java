package org.example.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "scheduled_jobs",
        indexes = {
                @Index(name = "idx_scheduled_jobs_due", columnList = "status, run_at"),
                @Index(name = "idx_scheduled_jobs_unique_key", columnList = "unique_key", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private JobType type;

    @Column(name = "unique_key", nullable = false, unique = true, length = 120)
    private String uniqueKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "run_at", nullable = false)
    private LocalDateTime runAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "recurring", nullable = false)
    @Builder.Default
    private boolean recurring = false;
}
