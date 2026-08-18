package org.example.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chat.entity.AddGroupMembersRequest;
import org.example.chat.entity.ChatMemberDto;
import org.example.chat.entity.ChatMessage;
import org.example.chat.entity.ChatMessageDto;
import org.example.chat.entity.ChatPhotoPageDto;
import org.example.chat.entity.ChatRoomDto;
import org.example.chat.entity.CreatePersonalGroupRequest;
import org.example.chat.entity.CreatePollRequest;
import org.example.chat.entity.PollVoteRequest;
import org.example.chat.entity.TypingSignalDto;
import org.example.chat.entity.UpdatePersonalGroupRequest;
import org.example.chat.entity.UserChatDto;
import org.example.chat.service.ChatAccess;
import org.example.chat.service.ChatMessageService;
import org.example.chat.service.ChatPinService;
import org.example.chat.service.ChatPollService;
import org.example.chat.service.ChatPreferenceService;
import org.example.chat.service.GroupChatService;
import com.example.common.metrics.AppMetrics;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.nio.file.AccessDeniedException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService messageService;
    private final GroupChatService groupChatService;
    private final ChatPollService pollService;
    private final ChatPreferenceService preferenceService;
    private final ChatPinService pinService;
    private final AppMetrics appMetrics;

    @PostMapping("/api/v1/social/chats/personal/init/{recipientId}")
    @ResponseBody
    public ResponseEntity<Void> initPersonalChat(
            @RequestHeader("X-User-Id") Long senderId,
            @PathVariable Long recipientId) throws AccessDeniedException {

        messageService.initializePersonalChat(senderId, recipientId);
        return ResponseEntity.ok().build();
    }

    @MessageMapping("/chat/forward-multiple")
    public void processMultipleMessages(@Payload List<ChatMessage> chatMessages, Principal principal)
            throws AccessDeniedException {
        Long userId = requireUserId(principal);
        if (chatMessages == null || chatMessages.isEmpty()) return;

        log.info("[WebSocket] Массовая пересылка {} сообщений от юзера ID: {}", chatMessages.size(), userId);

        for (ChatMessage msg : chatMessages) {
            msg.setSenderId(userId);
            msg.setTimestamp(LocalDateTime.now());
            try {
                ChatMessage savedMsg = messageService.saveMessage(msg);
                if (savedMsg.getForwardedFrom() != null) {
                    org.hibernate.Hibernate.initialize(savedMsg.getForwardedFrom());
                }
                broadcastSaved(savedMsg, savedMsg);
            } catch (Exception ex) {
                appMetrics.chatOshibka();
                log.warn("[Чат] Не удалось переслать сообщение: {}", ex.getMessage());
                throw ex;
            }
        }
    }

    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessage chatMessage, Principal principal) throws AccessDeniedException {
        Long userId = requireUserId(principal);
        chatMessage.setSenderId(userId);

        if (chatMessage.getContent() != null && "[MESSAGES_READ]".equals(chatMessage.getContent())) {
            Long recipientId = chatMessage.getRecipientId();
            if (recipientId != null && !recipientId.equals(userId)) {
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(recipientId),
                        "/queue/messages",
                        chatMessage
                );
            }
            return;
        }

        try {
            ChatMessage savedMsg = messageService.saveMessage(chatMessage);
            ChatMessageDto responseDto = messageService.convertToDto(savedMsg.getId());
            broadcastDto(responseDto);
        } catch (Exception ex) {
            appMetrics.chatOshibka();
            log.warn("[Чат] Не удалось отправить сообщение: {}", ex.getMessage());
            throw ex;
        }
    }

    @MessageMapping("/chat/typing")
    public void processTyping(@Payload TypingSignalDto signal, Principal principal) {
        Long userId = requireUserId(principal);
        if (signal == null) {
            return;
        }
        try {
            messageService.assertCanSignal(userId, signal.getChatId(), signal.getRecipientId());
        } catch (AccessDeniedException | SecurityException ex) {
            log.warn("[Чат] Typing без доступа userId={} chatId={} recipientId={}: {}",
                    userId, signal.getChatId(), signal.getRecipientId(), ex.getMessage());
            return;
        }
        signal.setSenderId(userId);
        signal.setContent("[TYPING]");

        if (signal.getChatId() != null) {
            messagingTemplate.convertAndSend("/topic/chat." + signal.getChatId(), signal);
            return;
        }
        if (signal.getRecipientId() != null) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(signal.getRecipientId()),
                    "/queue/messages",
                    signal
            );
        }
    }

    @GetMapping("/api/v1/social/chats/history/{recipientId}")
    @ResponseBody
    public List<ChatMessageDto> getChatHistory(
            @RequestHeader("X-User-Id") Long senderId,
            @PathVariable Long recipientId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "30") int size) {
        try {
            List<ChatMessageDto> page = messageService.getChatHistoryPage(senderId, recipientId, beforeId, size);
            appMetrics.chatIstoriyaUspeh();
            return page;
        } catch (Exception ex) {
            appMetrics.chatIstoriyaOshibka();
            log.warn("[Чат] Ошибка загрузки истории личного чата: {}", ex.getMessage());
            throw ex;
        }
    }

    @GetMapping("/api/v1/social/chats/history/{recipientId}/photos")
    @ResponseBody
    public ChatPhotoPageDto getPersonalChatPhotos(
            @RequestHeader("X-User-Id") Long senderId,
            @PathVariable Long recipientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return messageService.getPersonalChatPhotos(senderId, recipientId, page, size);
    }

    @GetMapping("/api/v1/social/chats/history/{recipientId}/files")
    @ResponseBody
    public ChatPhotoPageDto getPersonalChatFiles(
            @RequestHeader("X-User-Id") Long senderId,
            @PathVariable Long recipientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return messageService.getPersonalChatFiles(senderId, recipientId, page, size);
    }

    @GetMapping("/api/v1/social/chats/history/{recipientId}/voices")
    @ResponseBody
    public ChatPhotoPageDto getPersonalChatVoices(
            @RequestHeader("X-User-Id") Long senderId,
            @PathVariable Long recipientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return messageService.getPersonalChatVoices(senderId, recipientId, page, size);
    }

    @GetMapping("/api/v1/social/chats/unread/count")
    @ResponseBody
    public long getUnreadMessagesCount(@RequestHeader("X-User-Id") Long userId) {
        return messageService.getUnreadCount(userId);
    }

    @PutMapping("/api/v1/social/chats/read/{senderId}")
    @ResponseBody
    public void readMessages(
            @PathVariable Long senderId,
            @RequestHeader("X-User-Id") Long recipientId) {
        messageService.readAllMessagesFromUser(senderId, recipientId);
    }

    @PutMapping("/api/v1/social/chats/message/{messageId}")
    @ResponseBody
    public ChatMessage editMessage(
            @PathVariable Long messageId,
            @RequestBody String newContent,
            @RequestHeader("X-User-Id") Long senderId) {
        ChatMessage updatedMsg = messageService.editMessage(messageId, newContent, senderId);
        broadcastSaved(updatedMsg, updatedMsg);
        return updatedMsg;
    }

    @DeleteMapping("/api/v1/social/chats/message/{messageId}")
    @ResponseBody
    public void deleteMessage(
            @PathVariable Long messageId,
            @RequestHeader("X-User-Id") Long senderId) {
        messageService.deleteMessage(messageId, senderId);
        broadcastDeleted(messageId);
    }

    @DeleteMapping("/api/v1/social/chats/message/batch")
    @PostMapping("/api/v1/social/chats/message/batch")
    @ResponseBody
    public void deleteMessagesBatch(
            @RequestBody List<Long> messageIds,
            @RequestHeader("X-User-Id") Long senderId) {
        if (messageIds == null || messageIds.isEmpty()) return;

        List<Long> ids = messageIds.stream()
                .filter(id -> id != null && id > 0)
                .toList();
        if (ids.isEmpty()) return;

        messageService.deleteMessagesBatch(ids, senderId);
        groupChatService.deleteGroupMessagesBatch(ids, senderId);

        for (Long id : ids) {
            broadcastDeleted(id);
        }
    }

    @DeleteMapping("/api/v1/social/chats/clear/{recipientId}")
    @ResponseBody
    public void clearChat(
            @PathVariable Long recipientId,
            @RequestParam(value = "isDelete", defaultValue = "false") boolean isDelete,
            @RequestHeader("X-User-Id") Long senderId) {

        if (isDelete) {
            messageService.deletePersonalChatForUser(senderId, recipientId);
        } else {
            messageService.clearChat(senderId, recipientId);
        }
        // Очистка/удаление только для себя — партнёру WS-сигнал не шлём
    }

    @GetMapping("/api/v1/social/chats/users")
    @ResponseBody
    public List<UserChatDto> getChatUsers(@RequestHeader("X-User-Id") Long userId) {
        return messageService.getUserChats(userId);
    }

    @GetMapping("/api/v1/social/chats/rooms")
    @ResponseBody
    public List<ChatRoomDto> getMyGroupRooms(@RequestHeader("X-User-Id") Long userId) {
        return groupChatService.getMyGroupRooms(userId);
    }

    @PostMapping("/api/v1/social/chats/rooms/personal")
    @ResponseBody
    public ChatRoomDto createPersonalGroup(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false, defaultValue = "") String firstName,
            @RequestParam(required = false, defaultValue = "") String lastName,
            @RequestBody CreatePersonalGroupRequest request) {
        return groupChatService.createPersonalGroup(userId, firstName, lastName, request);
    }

    @PatchMapping("/api/v1/social/chats/rooms/{chatId}")
    @ResponseBody
    public ChatRoomDto updatePersonalGroup(
            @PathVariable Long chatId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false, defaultValue = "") String firstName,
            @RequestParam(required = false, defaultValue = "") String lastName,
            @RequestBody UpdatePersonalGroupRequest request) {
        return groupChatService.updatePersonalGroup(chatId, userId, firstName, lastName, request);
    }

    @GetMapping("/api/v1/social/chats/rooms/{chatId}/members")
    @ResponseBody
    public List<ChatMemberDto> listPersonalGroupMembers(
            @PathVariable Long chatId,
            @RequestHeader("X-User-Id") Long userId) {
        return groupChatService.listMembers(chatId, userId);
    }

    @PostMapping("/api/v1/social/chats/rooms/{chatId}/members")
    @ResponseBody
    public void addPersonalGroupMembers(
            @PathVariable Long chatId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false, defaultValue = "") String firstName,
            @RequestParam(required = false, defaultValue = "") String lastName,
            @RequestBody AddGroupMembersRequest request) {
        groupChatService.addMembers(chatId, userId, firstName, lastName, request);
    }

    @DeleteMapping("/api/v1/social/chats/rooms/{chatId}/members/{targetUserId}")
    @ResponseBody
    public void leaveOrKickPersonalGroupMember(
            @PathVariable Long chatId,
            @PathVariable Long targetUserId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false, defaultValue = "") String firstName,
            @RequestParam(required = false, defaultValue = "") String lastName) {
        if (userId.equals(targetUserId)) {
            groupChatService.leavePersonalGroup(chatId, userId, firstName, lastName);
        } else {
            groupChatService.kickPersonalGroupMember(chatId, userId, targetUserId, firstName, lastName);
        }
    }

    @GetMapping("/api/v1/social/chats/room/{chatId}/history")
    @ResponseBody
    public List<ChatMessageDto> getGroupChatHistory(
            @PathVariable Long chatId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "30") int size) {
        try {
            List<ChatMessageDto> page = groupChatService.getGroupChatHistoryPage(chatId, userId, beforeId, size);
            appMetrics.chatIstoriyaUspeh();
            return page;
        } catch (Exception ex) {
            appMetrics.chatIstoriyaOshibka();
            log.warn("[Чат] Ошибка загрузки истории группового чата: {}", ex.getMessage());
            throw ex;
        }
    }

    @GetMapping("/api/v1/social/chats/room/{chatId}/photos")
    @ResponseBody
    public ChatPhotoPageDto getGroupChatPhotos(
            @PathVariable Long chatId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return groupChatService.getGroupChatPhotos(chatId, userId, page, size);
    }

    @GetMapping("/api/v1/social/chats/room/{chatId}/files")
    @ResponseBody
    public ChatPhotoPageDto getGroupChatFiles(
            @PathVariable Long chatId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return groupChatService.getGroupChatFiles(chatId, userId, page, size);
    }

    @GetMapping("/api/v1/social/chats/room/{chatId}/voices")
    @ResponseBody
    public ChatPhotoPageDto getGroupChatVoices(
            @PathVariable Long chatId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return groupChatService.getGroupChatVoices(chatId, userId, page, size);
    }

    @PostMapping("/api/v1/social/chats/room/{chatId}/read")
    @ResponseBody
    public ResponseEntity<Void> markChatAsRead(
            @PathVariable Long chatId,
            @RequestHeader("X-User-Id") Long userId) {
        groupChatService.resetUnreadCount(chatId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/v1/social/chats/unread/total")
    @ResponseBody
    public long getTotalUnreadCount(@RequestHeader("X-User-Id") Long userId) {
        long personalUnread = messageService.getUnreadCount(userId);
        long groupUnread = groupChatService.getMyGroupRooms(userId).stream()
                .mapToLong(ChatRoomDto::getUnread)
                .sum();
        return personalUnread + groupUnread;
    }

    @PostMapping("/api/v1/social/chats/room/{chatId}/polls")
    @ResponseBody
    public ChatMessageDto createPoll(
            @PathVariable Long chatId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false, defaultValue = "") String firstName,
            @RequestParam(required = false, defaultValue = "") String lastName,
            @RequestBody @Valid CreatePollRequest request) throws AccessDeniedException {
        ChatMessageDto dto = pollService.createPoll(chatId, userId, firstName, lastName, request);
        broadcastDto(dto);
        return dto;
    }

    @PostMapping("/api/v1/social/chats/polls/{pollId}/vote")
    @ResponseBody
    public ChatMessageDto votePoll(
            @PathVariable Long pollId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody(required = false) PollVoteRequest request) throws AccessDeniedException {
        ChatMessageDto dto = pollService.vote(pollId, userId, request == null ? new PollVoteRequest() : request);
        broadcastDto(dto);
        return dto;
    }

    @PostMapping("/api/v1/social/chats/polls/{pollId}/close")
    @ResponseBody
    public ChatMessageDto closePoll(
            @PathVariable Long pollId,
            @RequestHeader("X-User-Id") Long userId) throws AccessDeniedException {
        ChatMessageDto dto = pollService.close(pollId, userId);
        broadcastDto(dto);
        return dto;
    }

    @PutMapping("/api/v1/social/chats/prefs/personal/{partnerId}/pin")
    @ResponseBody
    public void pinPersonalChat(@PathVariable Long partnerId, @RequestHeader("X-User-Id") Long userId) {
        preferenceService.pinPersonalChat(userId, partnerId);
    }

    @DeleteMapping("/api/v1/social/chats/prefs/personal/{partnerId}/pin")
    @ResponseBody
    public void unpinPersonalChat(@PathVariable Long partnerId, @RequestHeader("X-User-Id") Long userId) {
        preferenceService.unpin(userId, ChatAccess.personalScope(partnerId));
    }

    @PutMapping("/api/v1/social/chats/prefs/personal/{partnerId}/mute")
    @ResponseBody
    public void mutePersonalChat(@PathVariable Long partnerId, @RequestHeader("X-User-Id") Long userId) {
        preferenceService.setMuted(userId, ChatAccess.personalScope(partnerId), true);
    }

    @DeleteMapping("/api/v1/social/chats/prefs/personal/{partnerId}/mute")
    @ResponseBody
    public void unmutePersonalChat(@PathVariable Long partnerId, @RequestHeader("X-User-Id") Long userId) {
        preferenceService.setMuted(userId, ChatAccess.personalScope(partnerId), false);
    }

    @PutMapping("/api/v1/social/chats/rooms/{chatId}/pin")
    @ResponseBody
    public void pinRoomChat(@PathVariable Long chatId, @RequestHeader("X-User-Id") Long userId) {
        preferenceService.pinRoom(userId, chatId);
    }

    @DeleteMapping("/api/v1/social/chats/rooms/{chatId}/pin")
    @ResponseBody
    public void unpinRoomChat(@PathVariable Long chatId, @RequestHeader("X-User-Id") Long userId) {
        preferenceService.unpin(userId, ChatAccess.roomScope(chatId));
    }

    @PutMapping("/api/v1/social/chats/rooms/{chatId}/mute")
    @ResponseBody
    public void muteRoomChat(@PathVariable Long chatId, @RequestHeader("X-User-Id") Long userId) {
        preferenceService.setMuted(userId, ChatAccess.roomScope(chatId), true);
    }

    @DeleteMapping("/api/v1/social/chats/rooms/{chatId}/mute")
    @ResponseBody
    public void unmuteRoomChat(@PathVariable Long chatId, @RequestHeader("X-User-Id") Long userId) {
        preferenceService.setMuted(userId, ChatAccess.roomScope(chatId), false);
    }

    @GetMapping("/api/v1/social/chats/personal/{partnerId}/pins")
    @ResponseBody
    public List<org.example.chat.entity.PinnedMessageDto> listPersonalPins(
            @PathVariable Long partnerId, @RequestHeader("X-User-Id") Long userId) {
        return pinService.listPersonalPins(userId, partnerId);
    }

    @PostMapping("/api/v1/social/chats/personal/{partnerId}/pins/{messageId}")
    @ResponseBody
    public List<org.example.chat.entity.PinnedMessageDto> pinPersonalMessage(
            @PathVariable Long partnerId,
            @PathVariable Long messageId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false, defaultValue = "") String firstName,
            @RequestParam(required = false, defaultValue = "") String lastName) {
        return pinService.pinPersonalMessage(userId, partnerId, messageId, firstName, lastName);
    }

    @DeleteMapping("/api/v1/social/chats/personal/{partnerId}/pins/{messageId}")
    @ResponseBody
    public List<org.example.chat.entity.PinnedMessageDto> unpinPersonalMessage(
            @PathVariable Long partnerId, @PathVariable Long messageId, @RequestHeader("X-User-Id") Long userId) {
        return pinService.unpinPersonalMessage(userId, partnerId, messageId);
    }

    @GetMapping("/api/v1/social/chats/rooms/{chatId}/pins")
    @ResponseBody
    public List<org.example.chat.entity.PinnedMessageDto> listRoomPins(
            @PathVariable Long chatId, @RequestHeader("X-User-Id") Long userId) {
        return pinService.listRoomPins(chatId, userId);
    }

    @PostMapping("/api/v1/social/chats/rooms/{chatId}/pins/{messageId}")
    @ResponseBody
    public List<org.example.chat.entity.PinnedMessageDto> pinRoomMessage(
            @PathVariable Long chatId,
            @PathVariable Long messageId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false, defaultValue = "") String firstName,
            @RequestParam(required = false, defaultValue = "") String lastName,
            @RequestParam(defaultValue = "true") boolean notify) {
        return pinService.pinRoomMessage(chatId, messageId, userId, firstName, lastName, notify);
    }

    @DeleteMapping("/api/v1/social/chats/rooms/{chatId}/pins/{messageId}")
    @ResponseBody
    public List<org.example.chat.entity.PinnedMessageDto> unpinRoomMessage(
            @PathVariable Long chatId, @PathVariable Long messageId, @RequestHeader("X-User-Id") Long userId) {
        return pinService.unpinRoomMessage(chatId, messageId, userId);
    }

    @PostMapping("/api/v1/social/chats/rooms/{chatId}/admins/{targetUserId}")
    @ResponseBody
    public void grantAdmin(
            @PathVariable Long chatId,
            @PathVariable Long targetUserId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false, defaultValue = "") String firstName,
            @RequestParam(required = false, defaultValue = "") String lastName) {
        groupChatService.setMemberAdmin(chatId, userId, targetUserId, true, firstName, lastName);
    }

    @DeleteMapping("/api/v1/social/chats/rooms/{chatId}/admins/{targetUserId}")
    @ResponseBody
    public void revokeAdmin(
            @PathVariable Long chatId,
            @PathVariable Long targetUserId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false, defaultValue = "") String firstName,
            @RequestParam(required = false, defaultValue = "") String lastName) {
        groupChatService.setMemberAdmin(chatId, userId, targetUserId, false, firstName, lastName);
    }

    @PostMapping("/api/v1/social/chats/rooms/{chatId}/transfer/{newOwnerId}")
    @ResponseBody
    public ChatRoomDto transferPersonalGroup(
            @PathVariable Long chatId,
            @PathVariable Long newOwnerId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false, defaultValue = "") String firstName,
            @RequestParam(required = false, defaultValue = "") String lastName) {
        return groupChatService.transferPersonalGroup(chatId, userId, newOwnerId, firstName, lastName);
    }

    @DeleteMapping("/api/v1/social/chats/rooms/{chatId}")
    @ResponseBody
    public void deletePersonalGroup(@PathVariable Long chatId, @RequestHeader("X-User-Id") Long userId) {
        groupChatService.deletePersonalGroup(chatId, userId);
    }

    private static Long requireUserId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new IllegalArgumentException("WebSocket-сессия без пользователя");
        }
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Некорректный идентификатор пользователя");
        }
    }

    private void broadcastDto(ChatMessageDto dto) {
        if (dto.getChatId() != null) {
            messagingTemplate.convertAndSend("/topic/chat." + dto.getChatId(), dto);
            return;
        }
        messagingTemplate.convertAndSendToUser(
                String.valueOf(dto.getRecipientId()),
                "/queue/messages",
                dto
        );
        if (dto.getSenderId() != null && !dto.getSenderId().equals(dto.getRecipientId())) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(dto.getSenderId()),
                    "/queue/messages",
                    dto
            );
        }
    }

    private void broadcastDeleted(Long messageId) {
        try {
            ChatMessage msg = messageService.findById(messageId);
            ChatMessageDto dto = ChatMessageDto.builder()
                    .id(msg.getId())
                    .senderId(msg.getSenderId())
                    .recipientId(msg.getRecipientId())
                    .senderFirstName(msg.getSenderFirstName())
                    .senderLastName(msg.getSenderLastName())
                    .chatId(msg.getChatId())
                    .content(msg.getContent())
                    .deleted(true)
                    .timestamp(msg.getTimestamp())
                    .build();
            broadcastDto(dto);
        } catch (Exception ex) {
            log.warn("[Чат] Не удалось разослать удаление сообщения {}: {}", messageId, ex.getMessage());
        }
    }

    private void broadcastSaved(ChatMessage msg, Object payload) {
        if (msg.getChatId() != null) {
            messagingTemplate.convertAndSend("/topic/chat." + msg.getChatId(), payload);
            return;
        }
        if (msg.getRecipientId() != null) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(msg.getRecipientId()),
                    "/queue/messages",
                    payload
            );
        }
        if (msg.getSenderId() != null && !msg.getSenderId().equals(msg.getRecipientId())) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(msg.getSenderId()),
                    "/queue/messages",
                    payload
            );
        }
    }
}
