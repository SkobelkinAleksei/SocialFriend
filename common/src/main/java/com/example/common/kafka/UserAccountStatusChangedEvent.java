package com.example.common.kafka;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserAccountStatusChangedEvent {
    private Long userId;
    /** ACTIVE, DELETED, BANNED */
    private String status;
}
