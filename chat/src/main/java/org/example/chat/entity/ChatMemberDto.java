package org.example.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMemberDto {
    private Long userId;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private boolean owner;
    private boolean admin;
}
