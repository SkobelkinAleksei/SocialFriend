package org.example.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TypingSignalDto {
    private Long senderId;
    private Long recipientId;
    private Long chatId;
    private String senderFirstName;
    private String content;
}
