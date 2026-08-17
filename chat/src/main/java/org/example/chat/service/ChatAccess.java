package org.example.chat.service;

import lombok.RequiredArgsConstructor;
import org.example.chat.entity.ChatParticipant;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.ChatRoomType;
import org.example.chat.repository.ChatParticipantRepository;
import org.example.chat.repository.ChatRoomRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatAccess {
    private final ChatParticipantRepository participantRepository;
    private final ChatRoomRepository roomRepository;

    public static String personalScope(Long partnerId) {
        return "P:" + partnerId;
    }

    public static String roomScope(Long chatId) {
        return "R:" + chatId;
    }

    public static long dmMin(Long a, Long b) {
        return Math.min(a, b);
    }

    public static long dmMax(Long a, Long b) {
        return Math.max(a, b);
    }

    public ChatRoom requireRoom(Long chatId) {
        return roomRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Чат не найден"));
    }

    public ChatParticipant requireParticipant(Long chatId, Long userId) {
        return participantRepository.findByChatIdAndUserId(chatId, userId)
                .orElseThrow(() -> new SecurityException("Вы не участник этого чата"));
    }

    public boolean isOwner(ChatRoom room, Long userId) {
        return room != null && userId != null && userId.equals(room.getOwnerId());
    }

    public boolean isAdmin(ChatRoom room, Long userId) {
        if (isOwner(room, userId)) {
            return true;
        }
        return participantRepository.findByChatIdAndUserId(room.getId(), userId)
                .map(ChatParticipant::isAdmin)
                .orElse(false);
    }

    public boolean isJuniorAdmin(ChatRoom room, Long userId) {
        return !isOwner(room, userId) && isAdmin(room, userId);
    }

    public void assertCanPinMessage(ChatRoom room, Long userId) {
        requireParticipant(room.getId(), userId);
        if (room.getRoomType() == ChatRoomType.PERSONAL_GROUP || room.getRoomType() == ChatRoomType.EVENT) {
            if (!isAdmin(room, userId)) {
                throw new SecurityException("Закреплять сообщения могут только администраторы");
            }
        }
    }

    public void assertCanKick(ChatRoom room, Long actorId, Long targetId) {
        requireParticipant(room.getId(), actorId);
        if (actorId.equals(targetId)) {
            throw new IllegalArgumentException("Нельзя исключить себя — покиньте чат");
        }
        if (isOwner(room, targetId)) {
            throw new SecurityException("Нельзя исключить создателя чата");
        }
        boolean targetAdmin = isAdmin(room, targetId);
        if (isOwner(room, actorId)) {
            return;
        }
        if (isJuniorAdmin(room, actorId)) {
            if (targetAdmin) {
                throw new SecurityException("Администратора может исключить только создатель");
            }
            return;
        }
        throw new SecurityException("Исключать участников может только администратор");
    }

    public void assertOwner(ChatRoom room, Long userId) {
        if (!isOwner(room, userId)) {
            throw new SecurityException("Это может сделать только создатель чата");
        }
    }
}
