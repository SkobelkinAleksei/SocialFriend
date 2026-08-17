package org.example.chat.entity.admin;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AdminChatAroundDto {
    Long roomId;
    String chatKind;
    String title;
    Long highlightedId;
    String privacy;
    List<AdminChatLineDto> messages;
}
