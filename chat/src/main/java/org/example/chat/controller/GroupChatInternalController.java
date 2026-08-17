package org.example.chat.controller;

import lombok.RequiredArgsConstructor;
import org.example.chat.service.GroupChatService;
import org.springframework.web.bind.annotation.*;

/**
 * Внутренние S2S-эндпоинты (event → chat напрямую на :8087).
 * Не проксируются gateway (/api/v1/social/**).
 */
@RestController
@RequestMapping("/api/v1/internal/chats/room")
@RequiredArgsConstructor
public class GroupChatInternalController {

    private final GroupChatService groupChatService;

    @PostMapping
    public Long createGroupRoom(@RequestParam Long eventId, @RequestParam String title, @RequestParam Long ownerId) {
        return groupChatService.createGroupRoom(eventId, title, ownerId);
    }

    @PostMapping("/join")
    public void joinToGroupRoom(@RequestParam Long eventId, @RequestParam Long userId) {
        groupChatService.joinToGroupRoom(eventId, userId);
    }

    @DeleteMapping("/{eventId}/cancel")
    public void cancelGroupRoomByOwner(
            @PathVariable Long eventId,
            @RequestHeader("X-User-Id") Long ownerId) {
        groupChatService.cancelAndNotifyGroupRoom(eventId, ownerId);
    }

    @PostMapping("/{eventId}/system-message")
    public void postSystemMessage(
            @PathVariable Long eventId,
            @RequestBody(required = false) com.example.common.dto.scheduler.SystemMessageRequest request) {
        String content = request != null ? request.getContent() : null;
        groupChatService.postSystemMessageByEventId(eventId, content);
    }

    @PostMapping("/{eventId}/keep-poll")
    public void createKeepPoll(
            @PathVariable Long eventId,
            @RequestBody(required = false) com.example.common.dto.scheduler.KeepChatPollRequest request) {
        groupChatService.createKeepChatPollByEventId(eventId, request);
    }

    @PostMapping("/{eventId}/resolve-keep-poll")
    public boolean resolveKeepPoll(@PathVariable Long eventId) {
        return groupChatService.resolveKeepChatPollByEventId(eventId);
    }

    @PostMapping("/{eventId}/close")
    public void closeRoom(@PathVariable Long eventId) {
        groupChatService.closeRoomByEventId(eventId);
    }

    @DeleteMapping("/{eventId}")
    public void deleteRoom(@PathVariable Long eventId) {
        groupChatService.deleteRoomByEventId(eventId);
    }
}
