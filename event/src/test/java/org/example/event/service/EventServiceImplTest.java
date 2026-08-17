package org.example.event.service;

import com.example.common.dto.event.EventDto;
import com.example.common.metrics.AppMetrics;
import org.example.event.dto.CreateEventDto;
import org.example.event.entity.EventEntity;
import org.example.event.exception.EventAccessDeniedException;
import org.example.event.exception.EventNotFoundException;
import org.example.event.mapper.EventMapper;
import org.example.event.outbox.OutboxService;
import org.example.event.repository.EventParticipantRepository;
import org.example.event.repository.EventRepository;
import org.example.event.restClient.ChatRestClient;
import org.example.event.restClient.SchedulerRestClient;
import org.example.event.restClient.UserRestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventServiceImpl — права организатора и окно отмены")
public class EventServiceImplTest {

    @Mock
    private EventParticipantRepository participantRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventMapper eventMapper;
    @Mock
    private ChatRestClient chatRestClient;
    @Mock
    private SchedulerRestClient schedulerRestClient;
    @Mock
    private UserRestClient userRestClient;
    @Mock
    private EventLifecycleService eventLifecycleService;
    @Mock
    private OutboxService outboxService;
    @Mock
    private AppMetrics appMetrics;

    @InjectMocks
    private EventServiceImpl service;

    @Test
    @DisplayName("Нет встречи — EventNotFoundException")
    void missing() {
        when(eventRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(EventNotFoundException.class, () -> service.getEventById(5L, 1L));
    }

    @Test
    @DisplayName("Не организатор не может править")
    void updateForbidden() {
        EventEntity event = EventEntity.builder().id(1L).organizerId(2L).title("Йога").build();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(EventAccessDeniedException.class,
                () -> service.updateEvent(1L, CreateEventDto.builder().title("Новое").build(), 9L));
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Не организатор не может отменить")
    void deleteForbidden() {
        EventEntity event = EventEntity.builder().id(1L).organizerId(2L).build();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(EventAccessDeniedException.class, () -> service.deleteEvent(1L, 9L));
        verify(chatRestClient, never()).cancelChatRoom(any(), any());
    }

    @Test
    @DisplayName("Меньше 12 часов до начала — отмена запрещена")
    void cancelTooLate() {
        EventEntity event = EventEntity.builder()
                .id(1L)
                .organizerId(2L)
                .eventDate(LocalDateTime.now().plusHours(3))
                .build();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(IllegalArgumentException.class, () -> service.deleteEvent(1L, 2L));
        verify(eventRepository, never()).delete(any());
    }
}
