package org.example.chat.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserChatDto {
    private String id;          // ID собеседника (в виде строки для фронта)
    private String name;        // Имя и Фамилия собеседника
    private String avatar;      // Ссылка на аватар (можно пустую строку)
    private String last_message;// Текст последнего сообщения
    private String time;        // Время сообщения (например, "14:42")
    private int unread;         // Счетчик непрочитанных
    private boolean online;     // Статус онлайн
    private boolean isDeleted;  // Флаг, удалено ли последнее сообщение
    private Long lastMessageId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime lastSeenAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime actualTimestamp;

    private boolean muted;
    private boolean pinned;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime pinnedAt;
}