package org.example.chat.controller;

import lombok.RequiredArgsConstructor;
import org.example.chat.service.GroupChatService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/social/chats/room")
@RequiredArgsConstructor
public class GroupChatRoomController {

    private final GroupChatService groupChatService;

    /**
     * isLeave=false — очистка истории только для себя.
     * isLeave=true — выход только из чата (без вызова event; для leave события используйте event API).
     */
    @DeleteMapping("/{eventId}")
    public void clearOrLeaveGroupRoom(
            @PathVariable Long eventId,
            @RequestParam(value = "isLeave", defaultValue = "false") boolean isLeave,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false, defaultValue = "") String firstName,
            @RequestParam(required = false, defaultValue = "") String lastName) {

        if (isLeave) {
            groupChatService.leaveGroupRoom(eventId, userId, firstName, lastName);
        } else {
            groupChatService.clearGroupChatForUser(eventId, userId);
        }
    }
}
