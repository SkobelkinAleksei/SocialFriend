package org.example.event.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.event.entity.enums.VoteType;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "event_user_reputation_votes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_event_voter_target",
                        columnNames = {"event_id", "voter_id", "target_id"}
                )
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventUserReputationVoteEntity {

    // =========================================================================
    // СИСТЕМНЫЕ ИДЕНТИФИКАТОРЫ И ИНДЕКСЫ СВЯЗИ

    /** Уникальный идентификатор записи голосования в базе данных */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Идентификатор завершенного события, в рамках которого выставляется оценка */
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    // =========================================================================
    // УЧАСТНИКИ ПРОЦЕССА ГОЛОСОВАНИЯ

    /** Идентификатор пользователя, который выставляет оценку  */
    @Column(name = "voter_id", nullable = false)
    private Long voterId;

    /** Идентификатор пользователя, которому начисляется плюс или минус */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    // =========================================================================
    // ПАРАМЕТРЫ И ТИП ВЫСТАВЛЕННОГО ГОЛОСА

    /** Направление изменения репутации (например: PLUS, MINUS) */
    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false)
    private VoteType voteType;

    // =========================================================================
    // СИСТЕМНЫЙ АУДИТ И СЧЕТЧИКИ АНТИСПАМА

    /** Счетчик отправленных пуш-уведомлений об изменении рейтинга для защиты от спама */
    @Column(name = "notification_count", nullable = false)
    private int notificationCount = 0;

    /** Автоматическая временная метка фиксации голоса в базе данных */
    @CreationTimestamp
    @Column(columnDefinition = "TIMESTAMP(0)", name = "time_stamp")
    private LocalDateTime timeStamp;
}
