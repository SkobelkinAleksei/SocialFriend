package org.example.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long eventId;

    private String title;

    private Long ownerId;
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type")
    @Builder.Default
    private ChatRoomType roomType = ChatRoomType.EVENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "add_members_policy")
    @Builder.Default
    private ChatRoomPolicy addMembersPolicy = ChatRoomPolicy.OWNER_ONLY;

    @Enumerated(EnumType.STRING)
    @Column(name = "rename_policy")
    @Builder.Default
    private ChatRoomPolicy renamePolicy = ChatRoomPolicy.OWNER_ONLY;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "avatar_policy")
    @Builder.Default
    private ChatRoomPolicy avatarPolicy = ChatRoomPolicy.OWNER_ONLY;
}
