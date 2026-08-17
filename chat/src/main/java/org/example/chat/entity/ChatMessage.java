package org.example.chat.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long senderId;
    private Long recipientId;

    private String senderFirstName;
    private String senderLastName;
    private Long chatId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "chat_message_photos", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "photo_url", length = 255)
    @OrderColumn(name = "photo_index")
    private List<String> photos = new ArrayList<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "chat_message_files", joinColumns = @JoinColumn(name = "message_id"))
    @OrderColumn(name = "file_index")
    private List<ChatFileAttachment> files = new ArrayList<>();

    @Column(length = 255)
    private String voiceUrl;

    private Integer voiceDuration;

    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime timestamp;
    private boolean read = false;
    private boolean isSystem = false;
    private boolean edited = false;
    private boolean deleted = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "chat_message_replies",
            joinColumns = @JoinColumn(name = "message_id"),
            inverseJoinColumns = @JoinColumn(name = "parent_id")
    )
    @JsonIgnoreProperties({"replies", "parentIds"})
    private Set<ChatMessage> replies = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forwarded_from_id")
    @JsonIgnoreProperties({"replies", "parentIds", "forwardedFrom", "hibernateLazyInitializer", "handler"})
    private ChatMessage forwardedFrom;

    @Transient
    private List<Long> parentIds;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "chat_message_forward_bundles",
            joinColumns = @JoinColumn(name = "parent_message_id"),
            inverseJoinColumns = @JoinColumn(name = "forwarded_message_id")
    )
    private List<ChatMessage> bundledForwards = new ArrayList<>();
}
