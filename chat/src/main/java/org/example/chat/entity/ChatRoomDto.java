package org.example.chat.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDto {
    private Long id;
    private Long eventId;
    private String title;
    private Long ownerId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime createdAt;
    private Integer unread;
    private String lastMessage;
    private String time;
    private ChatRoomType roomType;
    private ChatRoomPolicy addMembersPolicy;
    private ChatRoomPolicy renamePolicy;
    private String avatarUrl;
    private ChatRoomPolicy avatarPolicy;
    private boolean muted;
    private boolean pinned;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime pinnedAt;
    private boolean admin;
}
