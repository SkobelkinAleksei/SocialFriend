package org.example.scheduler.service;

import com.example.common.dto.scheduler.EventLifecycleRequest;
import com.example.common.lifecycle.EventLifecycleSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.scheduler.entity.JobStatus;
import org.example.scheduler.entity.JobType;
import org.example.scheduler.entity.ScheduledJob;
import org.example.scheduler.repository.ScheduledJobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobScheduleService — таймеры встречи и отмена")
public class JobScheduleServiceTest {

    @Mock
    private ScheduledJobRepository jobRepository;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @Spy
    private EventLifecycleSettings lifecycleSettings = new EventLifecycleSettings();

    @InjectMocks
    private JobScheduleService service;

    @Test
    @DisplayName("uniqueKey стабилен: event:{id}:{type}")
    void uniqueKeyFormat() {
        assertEquals("event:12:event-delete-chat-after-cancel",
                JobScheduleService.uniqueKey(12L, JobType.EVENT_DELETE_CHAT_AFTER_CANCEL));
    }

    @Test
    @DisplayName("upsertLifecycle без даты — IllegalArgumentException")
    void lifecycleNeedsDate() {
        assertThrows(IllegalArgumentException.class,
                () -> service.upsertLifecycle(EventLifecycleRequest.builder().eventId(1L).build()));
    }

    @Test
    @DisplayName("scheduleCancel без eventId — ошибка")
    void cancelNeedsId() {
        assertThrows(IllegalArgumentException.class, () -> service.scheduleCancel(null));
    }

    @Test
    @DisplayName("Отмена встречи: живые таймеры CANCELLED, создаётся задача удаления чата")
    void cancelUpsertsDeleteJob() {
        ScheduledJob remind = ScheduledJob.builder()
                .uniqueKey("event:5:event-remind-1h")
                .status(JobStatus.PENDING)
                .type(JobType.EVENT_REMIND_1H)
                .build();
        when(jobRepository.findAllByUniqueKeyStartingWith("event:5:")).thenReturn(List.of(remind));
        when(jobRepository.findByUniqueKey("event:5:event-delete-chat-after-cancel")).thenReturn(Optional.empty());

        service.scheduleCancel(5L);

        assertEquals(JobStatus.CANCELLED, remind.getStatus());
        verify(jobRepository).saveAll(List.of(remind));
        verify(jobRepository).saveAndFlush(any(ScheduledJob.class));
    }

    @Test
    @DisplayName("Повторный keep-fail delete не перезаписывает уже PENDING задачу")
    void keepFailSkipIfPending() {
        ScheduledJob existing = ScheduledJob.builder()
                .uniqueKey("event:5:event-delete-chat-after-keep-fail")
                .status(JobStatus.PENDING)
                .type(JobType.EVENT_DELETE_CHAT_AFTER_KEEP_FAIL)
                .build();
        when(jobRepository.findByUniqueKey("event:5:event-delete-chat-after-keep-fail"))
                .thenReturn(Optional.of(existing));

        service.scheduleKeepFailDelete(5L);

        verify(jobRepository, never()).save(existing);
        verify(jobRepository, never()).saveAndFlush(any());
    }
}
