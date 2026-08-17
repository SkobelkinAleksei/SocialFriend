package org.example.event.dto;

import com.example.common.validation.NoProfanity;
import com.example.common.validation.NoMarketplaceListing;
import jakarta.validation.constraints.*;
import org.example.event.entity.enums.EventCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.event.validation.EventScheduleWindow;
import org.example.event.validation.EventTags;
import org.example.event.validation.OnCreate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventDto {

    @NotBlank(message = "Название события должно быть указано.")
    @Size(min = 3, max = 50, message = "Название события должно быть от 3 до 50 символов.")
    @NoProfanity(message = "В названии нельзя использовать ненормативную лексику.")
    @NoMarketplaceListing
    private String title;

    @Size(max = 2000, message = "Описание события не должно превышать 2000 символов.")
    @NoProfanity(message = "В описании нельзя использовать ненормативную лексику.")
    @NoMarketplaceListing
    private String description;

    @NotNull(message = "Категория события должна быть выбрана.")
    private EventCategory category;

    @Builder.Default
    private Boolean isPrivate = Boolean.FALSE;

    public Boolean getIsPrivate() {
        return Boolean.TRUE.equals(isPrivate);
    }

    @NotNull(message = "Широта (latitude) обязательна для установки маркера на карте.")
    @DecimalMin(value = "-90.0", message = "Некорректная широта.")
    @DecimalMax(value = "90.0", message = "Некорректная широта.")
    private BigDecimal latitude;

    @NotNull(message = "Долгота (longitude) обязательна для установки маркера на карте.")
    @DecimalMin(value = "-180.0", message = "Некорректная долгота.")
    @DecimalMax(value = "180.0", message = "Некорректная долгота.")
    private BigDecimal longitude;

    @NotBlank(message = "Адрес или название места должны быть указаны.")
    @Size(min = 3, max = 150, message = "Название места должно быть от 3 до 150 символов.")
    private String locationName;

    @NotNull(message = "Дата и время проведения события должны быть указаны.")
    @EventScheduleWindow(groups = OnCreate.class)
    private LocalDateTime eventDate;

    @NotNull(message = "Лимит участников должен быть указан.")
    @Min(value = 2, message = "Минимальный лимит участников — 2 человека.")
    @Max(value = 300, message = "Максимальный лимит участников — 300 человек.")
    private Integer participantLimit;

    @EventTags
    private List<String> tags;

    @Size(max = 5, message = "К событию можно прикрепить не больше 5 фотографий.")
    private List<String> photos;
}
