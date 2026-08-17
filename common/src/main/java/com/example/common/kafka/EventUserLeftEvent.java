package com.example.common.kafka;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventUserLeftEvent {
    private Long eventId;
    private Long userId;
    /** Статус после операции: NONE (leave), KICKED, REJECTED, BANNED и т.д. */
    private String status;
    /** true = принудительный кик организатором (убрать из чата) */
    @JsonProperty("isKicked")
    @JsonAlias({"kicked", "isKicked"})
    private boolean kicked;
    private String firstName;
    private String lastName;
}
