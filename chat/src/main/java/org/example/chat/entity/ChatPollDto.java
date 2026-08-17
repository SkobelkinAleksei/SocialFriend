package org.example.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatPollDto {
    private Long id;
    private Long messageId;
    private Long chatId;
    private Long creatorId;
    private String question;
    private boolean anonymous;
    private boolean multiple;
    private boolean closed;
    private String purpose;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private java.time.LocalDateTime closesAt;
    private int totalVotes;

    @Builder.Default
    private List<Option> options = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        private Long id;
        private String text;
        private int votes;
        private boolean selected;
        @Builder.Default
        private List<Voter> voters = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Voter {
        private Long userId;
        private String firstName;
        private String lastName;
        private String avatarUrl;
    }
}
