package com.example.common.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OutgoingRequestDto {
    private Long id;                 // ID события
    private String title;            // Название встречи
    private LocalDateTime eventDate; // Дата встречи
    private String locationName;     // Название локации
    private String status;           // Статус заявки: "PENDING" или "REJECTED"
    private Long organizerId;
}
