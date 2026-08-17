package com.example.common.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IncomingRequestDto {
    private Long participantId;      // ID записи из event_participants для PATCH-запроса
    private Long userId;             // ID самого соседа
    private String firstName;        // Имя из UserDto
    private String lastName;         // Фамилия из UserDto
    private String cityAndDistrict;  // Собранная строка "Город, Район"
    private Long reputation;      // Репутация соседа
    private String status;
}
