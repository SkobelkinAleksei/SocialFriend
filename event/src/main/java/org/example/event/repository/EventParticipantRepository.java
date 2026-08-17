package org.example.event.repository;

import org.example.event.entity.EventParticipantEntity;
import org.example.event.entity.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventParticipantRepository extends JpaRepository<EventParticipantEntity, Long> {

    // =========================================================================
    // ЗАПРОСЫ ПО КОНКРЕТНОМУ ПОЛЬЗОВАТЕЛЮ (ДЛЯ ПРОФИЛЕЙ И ИСХОДЯЩИХ ЗАЯВОК)

    /** Ищем связь конкретного юзера с конкретным событием */
    Optional<EventParticipantEntity> findByEventIdAndUserId(Long eventId, Long userId);

    /** Найти все записи участий для конкретного пользователя */
    List<EventParticipantEntity> findAllByUserId(Long userId);

    /** Добавляем этот запрос, чтобы достать только ID подтвержденных событий */
    @Query("SELECT p.eventId FROM EventParticipantEntity p WHERE p.userId = :userId AND p.status = :status")
    List<Long> findEventIdsByUserIdAndStatus(@Param("userId") Long userId, @Param("status") ParticipantStatus status);

    /** Найти активные исходящие заявки пользователя с фильтрацией по дате события */
    @Query("SELECT p FROM EventParticipantEntity p " +
            "JOIN EventEntity e ON p.eventId = e.id " +
            "WHERE p.userId = :userId " +
            "AND p.status IN :statuses " +
            "AND (e.eventDate IS NULL OR e.eventDate > CURRENT_TIMESTAMP)")
    List<EventParticipantEntity> findActiveOutgoingRequests(
            @Param("userId") Long userId,
            @Param("statuses") List<ParticipantStatus> statuses
    );


    // =========================================================================
    // ЗАПРОСЫ ПО СОБЫТИЯМ (ДЛЯ МОДЕРАЦИИ, СПИСКОВ И ПАКЕТНОЙ ОБРАБОТКИ)

    /** Найти всех участников для конкретного события с определенным статусом */
    List<EventParticipantEntity> findAllByEventIdAndStatus(Long eventId, ParticipantStatus participantStatus);

    /** Число участников события с данным статусом (для сверки счётчика) */
    long countByEventIdAndStatus(Long eventId, ParticipantStatus status);

    /** Найти участников для списка событий с определенным статусом */
    List<EventParticipantEntity> findByEventIdInAndStatus(List<Long> myEventIds, ParticipantStatus participantStatus);

    List<EventParticipantEntity> findByEventIdInAndUserId(List<Long> eventIds, Long userId);

    @Query("""
            SELECT p.eventId, COUNT(p)
            FROM EventParticipantEntity p
            WHERE p.eventId IN :eventIds AND p.status = :status
            GROUP BY p.eventId
            """)
    List<Object[]> countJoinedByEventIds(
            @Param("eventIds") List<Long> eventIds,
            @Param("status") ParticipantStatus status
    );

    /** Найти участников для списка событий, у которых статус находится в заданном списке */
    List<EventParticipantEntity> findByEventIdInAndStatusIn(List<Long> eventIds, List<ParticipantStatus> statuses);


    // =========================================================================
    // АГРЕГАЦИЯ СТАТИСТИКИ И МОДИФИКАЦИЯ ДАННЫХ (ДЕЛЕТЫ)

    /** Посчитать количество посещенных событий конкретного соседа до текущего момента времени */
    @Query("""
       SELECT COUNT(p) 
       FROM EventParticipantEntity p 
       JOIN EventEntity e ON p.eventId = e.id 
       WHERE p.userId = :neighborId 
       AND p.status = :status 
       AND e.eventDate < :now
       """)
    int countAttendedEvents(
            @Param("neighborId") Long neighborId,
            @Param("status") ParticipantStatus status,
            @Param("now") LocalDateTime now
    );

    /** Полная зачистка всех регистраций участников при удалении события */
    @Modifying
    @Query("DELETE FROM EventParticipantEntity ep WHERE ep.eventId = :eventId")
    void deleteAllByEventId(Long eventId);
}
