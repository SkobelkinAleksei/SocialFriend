package org.example.chat.entity;

import lombok.Data;

@Data
public class UpdatePersonalGroupRequest {
    private String title;
    private ChatRoomPolicy addMembersPolicy;
    private ChatRoomPolicy renamePolicy;
    private ChatRoomPolicy avatarPolicy;
    private String avatarUrl;
}
