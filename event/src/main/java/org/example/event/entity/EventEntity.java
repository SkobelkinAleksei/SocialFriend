package org.example.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.event.entity.enums.EventCategory;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "events")
public class EventEntity {

    // =========================================================================
    // СИСТЕМНЫЕ ИДЕНТИФИКАТОРЫ СУЩНОСТИ

    /** Уникальный идентификатор записи в базе данных */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    /** Уникальный идентификатор пользователя-создателя (организатора) встречи */
    @Column(name = "organizer_id", nullable = false)
    private Long organizerId;

    // =========================================================================
    // ОСНОВНАЯ ТЕКСТОВАЯ ИНФОРМАЦИЯ О СОБЫТИИ

    /** Краткое название встречи */
    @Column(name = "title", nullable = false, length = 50)
    private String title;

    /** Полное описание деталей встречи с поддержкой длинных текстов */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Тематическая категория встречи */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private EventCategory category;

    // =========================================================================
    // ПАРАМЕТРЫ ПРОВЕДЕНИЯ, СТАТУСЫ И ЛИМИТЫ МЕСТ

    /** Флаг приватности: true = строго по одобрению заявки, false = открытая встреча */
    @Column(name = "is_private", nullable = false)
    private boolean isPrivate;

    /** Дата и время проведения мероприятия */
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    /** Максимально допустимое количество участников встречи */
    @Column(name = "participant_limit", nullable = false)
    private Integer participantLimit;

    /** Текущее число одобренных и подтвержденных участников на данный момент */
    @Column(name = "current_participants", nullable = false)
    private Integer currentParticipants;

    // =========================================================================
    // ГЕОГРАФИЧЕСКОЕ ПОЗИЦИОНИРОВАНИЕ И АДРЕСАЦИЯ

    /** Географическая широта для точного позиционирования маркера на гео-карте */
    @Column(name = "latitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    /** Географическая долгота для точного позиционирования маркера на гео-карте */
    @Column(name = "longitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    /** Полнотекстовое текстовое описание адреса или названия локации встречи */
    @Column(name = "location_name", nullable = false, length = 150)
    private String locationName;

    // =========================================================================
    // СВЯЗАННЫЕ КОЛЛЕКЦИИ (МЕДИА И ТЕГИ)

    /** Коллекция текстовых тегов для дополнительной фильтрации */
    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "event_tags", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "tag", length = 30)
    private List<String> tags = new ArrayList<>();

    /** Список URL-ссылок на фотографии, прикрепленные создателем к карточке встречи */
    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "event_photos", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "photo_url", length = 255)
    private List<String> photos = new ArrayList<>();


    // =========================================================================
    // СИСТЕМНЫЙ АУДИТ ДАННЫХ
    // =========================================================================

    /** Автоматическая временная метка создания записи в базе данных */
    @CreationTimestamp
    @Column(columnDefinition = "TIMESTAMP(0)", name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}