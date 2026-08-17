package org.example.chat.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CreatePersonalGroupRequest {
    private String title;
    private List<Long> memberIds = new ArrayList<>();
    private ChatRoomPolicy addMembersPolicy = ChatRoomPolicy.OWNER_ONLY;
    private ChatRoomPolicy renamePolicy = ChatRoomPolicy.OWNER_ONLY;
    private ChatRoomPolicy avatarPolicy = ChatRoomPolicy.OWNER_ONLY;
    private String avatarUrl;
}
