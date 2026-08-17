package com.example.common.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {
    private Long id;
    private String title;
    private String description;
    private String category;
    private Boolean isPrivate;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String locationName;
    private Long organizerId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime eventDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    private Integer participantLimit;
    private Integer currentParticipants;
    private List<String> tags;
    private List<String> photos;
    private boolean isPast;
    /** Окно оценок открыто прямо сейчас. */
    private boolean canVoteReputation;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime reputationOpensAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime reputationClosesAt;
    /** JOINED / PENDING / NONE / … для текущего зрителя. Организатор всегда JOINED. */
    private String userStatus;
    private String organizerName;
    private String organizerAvatarUrl;
    private List<EventPersonPreviewDto> peoplePreview;

    public boolean isPast() {
        if (this.eventDate == null) {
            return false;
        }
        return this.eventDate.isBefore(LocalDateTime.now());
    }
}
