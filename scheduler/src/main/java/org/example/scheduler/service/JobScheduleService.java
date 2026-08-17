package org.example.scheduler.service;

import com.example.common.dto.scheduler.EventLifecycleRequest;
import com.example.common.lifecycle.EventLifecycleSettings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.scheduler.entity.JobStatus;
import org.example.scheduler.entity.JobType;
import org.example.scheduler.entity.ScheduledJob;
import org.example.scheduler.repository.ScheduledJobRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobScheduleService {

    private final ScheduledJobRepository jobRepository;
    private final ObjectMapper objectMapper;
    private final EventLifecycleSettings lifecycleSettings;

    public static String eventPrefix(Long eventId) {
        return "event:" + eventId + ":";
    }

    public static String uniqueKey(Long eventId, JobType type) {
        return eventPrefix(eventId) + type.name().toLowerCase().replace('_', '-');
    }

    @Transactional
    public void upsertLifecycle(EventLifecycleRequest request) {
        if (request == null || request.getEventId() == null || request.getEventDate() == null) {
            throw new IllegalArgumentException("Для таймеров встречи нужны eventId и eventDate");
        }
        LocalDateTime now = LocalDateTime.now();
        String payload = writePayload(request);

        upsertIfPending(
                uniqueKey(request.getEventId(), JobType.EVENT_REMIND_1H),
                JobType.EVENT_REMIND_1H,
                lifecycleSettings.remindAt(request.getEventDate()),
                payload,
                now,
                true
        );
        upsertIfPending(
                uniqueKey(request.getEventId(), JobType.EVENT_STARTED),
                JobType.EVENT_STARTED,
                request.getEventDate(),
                payload,
                now,
                false
        );
        upsertIfPending(
                uniqueKey(request.getEventId(), JobType.EVENT_OPEN_REPUTATION),
                JobType.EVENT_OPEN_REPUTATION,
                lifecycleSettings.reputationOpensAt(request.getEventDate()),
                payload,
                now,
                false
        );
        upsertIfPending(
                uniqueKey(request.getEventId(), JobType.EVENT_KEEP_CHAT_POLL),
                JobType.EVENT_KEEP_CHAT_POLL,
                lifecycleSettings.keepPollAt(request.getEventDate()),
                payload,
                now,
                false
        );
        upsertIfPending(
                uniqueKey(request.getEventId(), JobType.EVENT_RESOLVE_KEEP_CHAT),
                JobType.EVENT_RESOLVE_KEEP_CHAT,
                lifecycleSettings.keepResolveAt(request.getEventDate()),
                payload,
                now,
                false
        );
        upsertIfPending(
                uniqueKey(request.getEventId(), JobType.EVENT_CLOSE_REPUTATION),
                JobType.EVENT_CLOSE_REPUTATION,
                lifecycleSettings.reputationClosesAt(request.getEventDate()),
                payload,
                now,
                false
        );
    }

    @Transactional
    public void resetLifecycle(EventLifecycleRequest request) {
        if (request == null || request.getEventId() == null) {
            throw new IllegalArgumentException("Для сброса таймеров нужен eventId");
        }
        deleteByEventId(request.getEventId());
        upsertLifecycle(request);
    }

    @Transactional
    public void scheduleCancel(Long eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Для отмены встречи нужен eventId");
        }
        cancelByEventId(eventId);
        LocalDateTime runAt = LocalDateTime.now().plusMinutes(lifecycleSettings.getCancelDeleteMinutes());
        String payload = writePayload(EventLifecycleRequest.builder().eventId(eventId).build());
        upsertByUniqueKey(
                uniqueKey(eventId, JobType.EVENT_DELETE_CHAT_AFTER_CANCEL),
                JobType.EVENT_DELETE_CHAT_AFTER_CANCEL,
                runAt,
                payload,
                false
        );
        log.info("[Scheduler] Чат встречи {} будет удалён в {}", eventId, runAt);
    }

    @Transactional
    public void scheduleKeepFailDelete(Long eventId) {
        if (eventId == null) {
            return;
        }
        LocalDateTime runAt = LocalDateTime.now().plusMinutes(lifecycleSettings.getKeepFailDeleteMinutes());
        String payload = writePayload(EventLifecycleRequest.builder().eventId(eventId).build());
        upsertByUniqueKey(
                uniqueKey(eventId, JobType.EVENT_DELETE_CHAT_AFTER_KEEP_FAIL),
                JobType.EVENT_DELETE_CHAT_AFTER_KEEP_FAIL,
                runAt,
                payload,
                true
        );
        log.info("[Scheduler] Чат встречи {} будет удалён после провала опроса в {}", eventId, runAt);
    }

    @Transactional
    public void ensureRefreshTokenCleanupJob() {
        String key = "system:refresh-token-cleanup";
        if (jobRepository.findByUniqueKey(key).isPresent()) {
            return;
        }
        try {
            jobRepository.saveAndFlush(ScheduledJob.builder()
                    .type(JobType.REFRESH_TOKEN_CLEANUP)
                    .uniqueKey(key)
                    .payload("{}")
                    .runAt(LocalDateTime.now().plusMinutes(5))
                    .status(JobStatus.PENDING)
                    .recurring(true)
                    .attempts(0)
                    .build());
            log.info("[Scheduler] Поставлена ежедневная очистка refresh-токенов");
        } catch (DataIntegrityViolationException ignored) {
            log.info("[Scheduler] Очистка refresh-токенов уже стоит в очереди");
        }
    }

    private void upsertIfPending(
            String uniqueKey,
            JobType type,
            LocalDateTime runAt,
            String payload,
            LocalDateTime now,
            boolean skipIfAlreadyDue
    ) {
        if (skipIfAlreadyDue && !runAt.isAfter(now)) {
            jobRepository.findByUniqueKey(uniqueKey).ifPresent(existing -> {
                if (existing.getStatus() == JobStatus.PENDING || existing.getStatus() == JobStatus.FAILED) {
                    existing.setStatus(JobStatus.CANCELLED);
                    existing.setLockedAt(null);
                    jobRepository.save(existing);
                }
            });
            return;
        }
        jobRepository.findByUniqueKey(uniqueKey).ifPresentOrElse(existing -> {
            if (existing.getStatus() == JobStatus.PENDING || existing.getStatus() == JobStatus.FAILED) {
                existing.setStatus(JobStatus.PENDING);
                existing.setRunAt(runAt);
                existing.setPayload(payload);
                existing.setAttempts(0);
                existing.setLockedAt(null);
                existing.setLastError(null);
                jobRepository.save(existing);
            }
        }, () -> saveNewJob(type, uniqueKey, payload, runAt, false));
    }

    private void upsertByUniqueKey(
            String key,
            JobType type,
            LocalDateTime runAt,
            String payload,
            boolean skipIfActiveOrDone
    ) {
        jobRepository.findByUniqueKey(key).ifPresentOrElse(existing -> {
            JobStatus status = existing.getStatus();
            if (skipIfActiveOrDone
                    && (status == JobStatus.PENDING || status == JobStatus.RUNNING || status == JobStatus.DONE)) {
                return;
            }
            if (status == JobStatus.DONE) {
                return;
            }
            existing.setType(type);
            existing.setStatus(JobStatus.PENDING);
            existing.setRunAt(runAt);
            existing.setPayload(payload);
            existing.setAttempts(0);
            existing.setLockedAt(null);
            existing.setLastError(null);
            jobRepository.save(existing);
        }, () -> saveNewJob(type, key, payload, runAt, false));
    }

    private void saveNewJob(JobType type, String uniqueKey, String payload, LocalDateTime runAt, boolean recurring) {
        try {
            jobRepository.saveAndFlush(ScheduledJob.builder()
                    .type(type)
                    .uniqueKey(uniqueKey)
                    .payload(payload)
                    .runAt(runAt)
                    .status(JobStatus.PENDING)
                    .recurring(recurring)
                    .attempts(0)
                    .build());
        } catch (DataIntegrityViolationException ignored) {
            log.info("[Scheduler] Задача {} уже есть, повторно не создаём", uniqueKey);
        }
    }

    private void deleteByEventId(Long eventId) {
        List<ScheduledJob> jobs = jobRepository.findAllByUniqueKeyStartingWith(eventPrefix(eventId));
        jobRepository.deleteAll(jobs);
    }

    private void cancelByEventId(Long eventId) {
        List<ScheduledJob> jobs = jobRepository.findAllByUniqueKeyStartingWith(eventPrefix(eventId));
        for (ScheduledJob job : jobs) {
            if (job.getStatus() == JobStatus.PENDING || job.getStatus() == JobStatus.RUNNING) {
                job.setStatus(JobStatus.CANCELLED);
            }
        }
        jobRepository.saveAll(jobs);
    }

    private String writePayload(EventLifecycleRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Не удалось сериализовать задачу планировщика", e);
        }
    }
}
