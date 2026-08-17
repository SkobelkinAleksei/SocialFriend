package org.example.chat.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;
    private Long senderId;
    private Long recipientId;
    private String senderFirstName;
    private String senderLastName;
    private String senderAvatarUrl;
    private Long chatId;
    private String content;
    @Builder.Default
    private List<String> photos = new ArrayList<>();
    @Builder.Default
    private List<ChatFileAttachment> files = new ArrayList<>();
    private String voiceUrl;
    private Integer voiceDuration;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime timestamp;
    private boolean read;
    private boolean isSystem;
    private boolean edited;
    private boolean deleted;
    private Set<Long> parentIds;
    private ForwardedFromDto forwardedFrom;
    @Builder.Default
    private List<ForwardedFromDto> bundledForwards = new ArrayList<>();
    private ChatPollDto poll;
    /** Vote/close broadcast: update the existing poll card, do not bump unread. */
    @com.fasterxml.jackson.annotation.JsonProperty("pollUpdate")
    private boolean pollUpdate;
    private boolean roomDeleted;
    private boolean pinned;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForwardedFromDto {
        private Long id;
        private Long senderId;
        private String senderFirstName;
        private String senderLastName;
        private String content;
        @Builder.Default
        private List<String> photos = new ArrayList<>();
        @Builder.Default
        private List<ChatFileAttachment> files = new ArrayList<>();
        private String voiceUrl;
        private Integer voiceDuration;
    }
}
