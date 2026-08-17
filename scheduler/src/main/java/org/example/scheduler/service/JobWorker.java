package org.example.scheduler.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.scheduler.entity.JobStatus;
import org.example.scheduler.entity.ScheduledJob;
import org.example.scheduler.repository.ScheduledJobRepository;
import com.example.common.metrics.AppMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobWorker {

    private final ScheduledJobRepository jobRepository;
    private final JobExecutor jobExecutor;
    private final TransactionTemplate transactionTemplate;
    private final AppMetrics appMetrics;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.scheduler.batch-size:20}")
    private int batchSize;

    @Value("${app.scheduler.max-attempts:8}")
    private int maxAttempts;

    @Value("${app.scheduler.stuck-running-minutes:5}")
    private int stuckRunningMinutes;

    @Scheduled(fixedDelayString = "${app.scheduler.poll-ms:5000}")
    public void poll() {
        List<Long> ids = claimDueIds();
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            processOne(id);
        }
    }

    @Scheduled(fixedDelay = 60_000)
    public void recoverStuck() {
        LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(stuckRunningMinutes);
        Integer recovered = transactionTemplate.execute(status -> jobRepository.recoverStuckRunning(staleBefore));
        if (recovered != null && recovered > 0) {
            log.warn("[Scheduler] Вернули в очередь {} зависших задач", recovered);
        }
    }

    private List<Long> claimDueIds() {
        return transactionTemplate.execute(status -> {
            @SuppressWarnings("unchecked")
            List<Number> rows = entityManager.createNativeQuery("""
                            WITH due AS (
                                SELECT id FROM scheduled_jobs
                                WHERE status = 'PENDING' AND run_at <= :now
                                ORDER BY run_at
                                LIMIT :batch
                                FOR UPDATE SKIP LOCKED
                            )
                            UPDATE scheduled_jobs j
                            SET status = 'RUNNING', locked_at = :now, attempts = attempts + 1
                            FROM due
                            WHERE j.id = due.id
                            RETURNING j.id
                            """)
                    .setParameter("now", LocalDateTime.now())
                    .setParameter("batch", batchSize)
                    .getResultList();
            return rows.stream().map(Number::longValue).toList();
        });
    }

    private void processOne(Long id) {
        ScheduledJob job = jobRepository.findById(id).orElse(null);
        if (job == null || job.getStatus() == JobStatus.CANCELLED) {
            return;
        }
        try {
            jobExecutor.execute(job);
            transactionTemplate.executeWithoutResult(status -> {
                ScheduledJob current = jobRepository.findById(id).orElse(null);
                if (current == null || current.getStatus() == JobStatus.CANCELLED) {
                    return;
                }
                if (current.isRecurring()) {
                    current.setStatus(JobStatus.PENDING);
                    current.setRunAt(LocalDateTime.now().plusDays(1));
                    current.setAttempts(0);
                    current.setLockedAt(null);
                    current.setLastError(null);
                } else {
                    current.setStatus(JobStatus.DONE);
                    current.setLockedAt(null);
                    current.setLastError(null);
                }
                jobRepository.save(current);
            });
            appMetrics.dzhobUspeh();
            log.info("[Планировщик] Задача {} выполнена", id);
        } catch (Exception ex) {
            appMetrics.dzhobOshibka();
            log.error("[Планировщик] Задача {} ({}) упала: {}", id, job.getType(), ex.getMessage());
            transactionTemplate.executeWithoutResult(status -> markFailure(id, ex));
        }
    }

    private void markFailure(Long id, Exception ex) {
        ScheduledJob current = jobRepository.findById(id).orElse(null);
        if (current == null || current.getStatus() == JobStatus.CANCELLED) {
            return;
        }
        current.setLastError(trimError(ex));
        current.setLockedAt(null);
        if (current.getAttempts() >= maxAttempts) {
            current.setStatus(JobStatus.FAILED);
        } else {
            current.setStatus(JobStatus.PENDING);
            int minutes = Math.min(30, 2 * current.getAttempts());
            current.setRunAt(LocalDateTime.now().plusMinutes(Math.max(1, minutes)));
        }
        jobRepository.save(current);
    }

    private static String trimError(Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
