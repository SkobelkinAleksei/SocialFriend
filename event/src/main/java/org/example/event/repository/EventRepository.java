package org.example.event.repository;

import org.example.event.entity.enums.EventCategory;
import org.example.event.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {

  // =========================================================================
  // ГЕО-ЗАПРОСЫ ДЛЯ ИНТЕРАКТИВНОЙ КАРТЫ (ВЫБОРКИ ПО КООРДИНАТАМ И КАТЕГОРИЯМ)

  /** Классический поиск по категории (для бокового списка "SOS", "Движ", "События") */
  List<EventEntity> findAllByCategory(EventCategory category);

  /** Ищет точки только в "квадрате" видимости, который пришлет фронтенд */
  @Query("SELECT e FROM EventEntity e WHERE e.latitude BETWEEN :minLat AND :maxLat AND e.longitude BETWEEN :minLng AND :maxLng")
  List<EventEntity> findEventsInBounds(
          @Param("minLat") BigDecimal minLat,
          @Param("maxLat") BigDecimal maxLat,
          @Param("minLng") BigDecimal minLng,
          @Param("maxLng") BigDecimal maxLng
  );

  /** Комбинированный метод: и в границах карты, и по конкретной категории */
  @Query("SELECT e FROM EventEntity e WHERE e.category = :category AND e.latitude BETWEEN :minLat AND :maxLat AND e.longitude BETWEEN :minLng AND :maxLng")
  List<EventEntity> findEventsInBoundsAndCategory(
          @Param("category") EventCategory category,
          @Param("minLat") BigDecimal minLat,
          @Param("maxLat") BigDecimal maxLat,
          @Param("minLng") BigDecimal minLng,
          @Param("maxLng") BigDecimal maxLng
  );

  // =========================================================================
  // АРХИВЫ АКТИВНОСТИ И ФИЛЬТРЫ ДЛЯ ЛИЧНОГО ПРОФИЛЯ ПОЛЬЗОВАТЕЛЯ

  /** Будущие встречи, в которых пользователь участвует (передаем список ID событий, где он состоит) */
  @Query("SELECT e FROM EventEntity e WHERE e.eventDate > :now " +
          "AND (e.organizerId = :userId OR e.id IN :joinedEventIds)")
  List<EventEntity> findUpcomingEventsAsParticipantOnly(
          @Param("userId") Long userId,
          @Param("now") LocalDateTime now,
          @Param("joinedEventIds") List<Long> joinedEventIds
  );

  /** Все прошедшие события юзера (где он был создателем ИЛИ где он участвовал из переданного списка) */
  @Query("SELECT e FROM EventEntity e WHERE e.eventDate <= :now " +
          "AND (e.organizerId = :userId OR (e.id IN :joinedEventIds))")
  List<EventEntity> findPastUserActivity(
          @Param("userId") Long userId,
          @Param("now") LocalDateTime now,
          @Param("joinedEventIds") List<Long> joinedEventIds
  );

  /** Предстоящие события, созданные строго этим юзером */
  @Query("SELECT e FROM EventEntity e WHERE e.eventDate > :now AND e.organizerId = :userId")
  List<EventEntity> findUpcomingEventsAsOrganizer(
          @Param("userId") Long userId,
          @Param("now") LocalDateTime now
  );

  /** Прошедшие события, созданные строго этим юзером */
  @Query("SELECT e FROM EventEntity e WHERE e.eventDate <= :now AND e.organizerId = :userId")
  List<EventEntity> findPastEventsAsOrganizer(
          @Param("userId") Long userId,
          @Param("now") LocalDateTime now
  );

  @Query("SELECT COUNT(e) FROM EventEntity e WHERE e.eventDate > :now "
          + "AND (e.organizerId = :userId OR e.id IN :joinedEventIds)")
  long countUpcomingEventsAsParticipantOnly(
          @Param("userId") Long userId,
          @Param("now") LocalDateTime now,
          @Param("joinedEventIds") List<Long> joinedEventIds
  );

  @Query("SELECT COUNT(e) FROM EventEntity e WHERE e.eventDate <= :now "
          + "AND (e.organizerId = :userId OR e.id IN :joinedEventIds)")
  long countPastUserActivity(
          @Param("userId") Long userId,
          @Param("now") LocalDateTime now,
          @Param("joinedEventIds") List<Long> joinedEventIds
  );

  // =========================================================================
  // ПУБЛИЧНЫЕ СПИСКИ И ФИЛЬТРЫ ДЛЯ ПРОФИЛЯ ДРУГОГО ПОЛЬЗОВАТЕЛЯ (СОСЕДА)

  /** Найти абсолютно все события по ID организатора */
  List<EventEntity> findByOrganizerId(Long organizerId);

  /** Выборка будущих встреч конкретного соседа, отсортированная по дате начала (по возрастанию) */
  List<EventEntity> findAllByOrganizerIdAndEventDateAfterOrderByEventDateAsc(Long organizerId, LocalDateTime date);

  /** Выборка прошедших встреч конкретного соседа, отсортированная по дате начала (по убыванию) */
  List<EventEntity> findAllByOrganizerIdAndEventDateBeforeOrderByEventDateDesc(Long organizerId, LocalDateTime date);

  // =========================================================================
  // СЧЕТЧИКИ И АГРЕГАЦИЯ ДАННЫХ ДЛЯ АНАЛИТИКИ ПРОФИЛЕЙ

  /** Подсчитать общее количество событий, созданных конкретным пользователем */
  int countByOrganizerId(Long organizerId);

  /** Подсчитать количество прошедших событий, организованных конкретным соседом */
  int countByOrganizerIdAndEventDateBefore(Long neighborId, LocalDateTime now);

  int countByOrganizerIdAndEventDateAfter(Long organizerId, LocalDateTime now);

  List<EventEntity> findByEventDateGreaterThanEqual(LocalDateTime from);

  /** Атомарно +1 участник при наличии свободных мест. 0 = лимит исчерпан / событие не найдено */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
          UPDATE EventEntity e
          SET e.currentParticipants = e.currentParticipants + 1
          WHERE e.id = :eventId
            AND e.currentParticipants < e.participantLimit
          """)
  int tryIncrementParticipants(@Param("eventId") Long eventId);

  /** Атомарно -1 участник (не ниже 1 — слот организатора) */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
          UPDATE EventEntity e
          SET e.currentParticipants = e.currentParticipants - 1
          WHERE e.id = :eventId
            AND e.currentParticipants > 1
          """)
  int tryDecrementParticipants(@Param("eventId") Long eventId);

  /** Выставить точное число участников (организатор + JOINED) */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
          UPDATE EventEntity e
          SET e.currentParticipants = :count
          WHERE e.id = :eventId
          """)
  int setCurrentParticipants(@Param("eventId") Long eventId, @Param("count") int count);
}