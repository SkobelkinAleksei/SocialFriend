package org.example.event.service;

import com.example.common.dto.event.EventDto;
import org.example.event.dto.CreateEventDto;
import org.example.event.dto.MyEventTabCountersDto;
import org.example.event.dto.NeighborEventCountersDto;
import org.example.event.entity.enums.EventCategory;
import org.example.event.entity.enums.EventUserFilter;
import org.example.event.entity.enums.NeighborEventFilter;

import java.math.BigDecimal;
import java.util.List;

public interface EventService {

    // =========================================================================
    // УПРАВЛЕНИЕ ЖИЗНЕННЫМ ЦИКЛОМ СОБЫТИЯ

    /** Поставить новую точку на карту (создать встречу) */
    EventDto createEvent(CreateEventDto dto, Long organizerId);

    /** Посмотреть детальную информацию о конкретном событии */
    EventDto getEventById(Long id, Long viewerId);

    /** Отредактировать данные существующего события */
    EventDto updateEvent(Long id, CreateEventDto dto, Long currentUserId);

    /** Снять точку с карты (удалить встречу организатором) */
    void deleteEvent(Long id, Long currentUserId);

    // =========================================================================
    // ВЗАИМОДЕЙСТВИЕ С ГЕО-КАРТОЙ И ОБЩИМ ТРАФИКОМ

    /** Загрузить маркеры встреч в видимой зоне карты с учетом выбранной категории */
    List<EventDto> getEvents(
            EventCategory category,
            BigDecimal minLat, BigDecimal maxLat,
            BigDecimal minLng, BigDecimal maxLng,
            Long viewerId
    );

    // =========================================================================
    // ФИЛЬТРАЦИЯ И СТАТИСТИКА СОБЫТИЙ ДЛЯ ЛИЧНОГО И ЧУЖИХ ПРОФИЛЕЙ

    /** Получить списки будущих или прошедших событий текущего авторизованного пользователя */
    List<EventDto> getEventsByEnumFilter(Long currentUserId, EventUserFilter filter);

    /** Получить публичный список прошедших или будущих событий другого пользователя */
    List<EventDto> getPublicEventsByNeighborId(Long neighborId, NeighborEventFilter filter, Long viewerId);

    /** Счётчики вкладок «События» для текущего пользователя */
    MyEventTabCountersDto getMyTabCounters(Long userId);

    /** Собрать счетчики созданных и посещенных событий для аналитики в профиле пользователя */
    NeighborEventCountersDto getEventCountersByUserId(Long neighborId);
}