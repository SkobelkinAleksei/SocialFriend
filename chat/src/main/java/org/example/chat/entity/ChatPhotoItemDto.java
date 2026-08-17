package org.example.chat.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatPhotoItemDto {
    private Long messageId;
    private String url;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime timestamp;

    private String name;
    private Long size;
    private String mimeType;
    private Integer duration;
    private Long senderId;
    private String senderFirstName;
    private String senderLastName;

    public ChatPhotoItemDto(Long messageId, String url, LocalDateTime timestamp) {
        this.messageId = messageId;
        this.url = url;
        this.timestamp = timestamp;
    }
}
