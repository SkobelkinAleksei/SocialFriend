package org.example.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.event.entity.enums.ParticipantStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "event_participants",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"event_id", "user_id"})})
public class EventParticipantEntity {

    // =========================================================================
    // СИСТЕМНЫЕ ИДЕНТИФИКАТОРЫ И КЛЮЧИ СВЯЗИ

    /** Уникальный идентификатор записи регистрации в базе данных */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Идентификатор события, на которое подана заявка или оформлено участие */
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    /** Идентификатор пользователя-участника */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // =========================================================================
    // БИЗНЕС-СТАТУС УЧАСТИЯ

    /** Текущий статус связи (например: PENDING, JOINED, REJECTED) */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ParticipantStatus status;

    // =========================================================================
    // СИСТЕМНЫЙ АУДИТ ДАННЫХ

    /** Автоматическая временная метка создания записи (подачи заявки/вступления) */
    @CreationTimestamp
    @Column(columnDefinition = "TIMESTAMP(0)", name = "created_at")
    private LocalDateTime createdAt;
}