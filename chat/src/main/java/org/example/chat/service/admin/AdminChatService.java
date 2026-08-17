package org.example.chat.service.admin;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chat.entity.ChatMessage;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.admin.AdminChatAroundDto;
import org.example.chat.entity.admin.AdminChatLineDto;
import org.example.chat.repository.ChatMessageRepository;
import org.example.chat.repository.ChatParticipantRepository;
import org.example.chat.repository.ChatRoomRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminChatService {

    private static final int WINDOW = 3;

    private final ChatMessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;

    @Transactional(readOnly = true)
    public AdminChatAroundDto around(Long messageId) {
        ChatMessage target = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Сообщение не найдено"));
        PageRequest page = PageRequest.of(0, WINDOW);
        List<ChatMessage> before;
        List<ChatMessage> after;
        String kind;
        String title;
        Long roomId;
        String privacy;
        if (target.getChatId() != null) {
            kind = "GROUP";
            roomId = target.getChatId();
            ChatRoom room = chatRoomRepository.findById(roomId).orElse(null);
            title = room == null ? "Группа" : room.getTitle();
            privacy = "Группа, админа в участниках нет";
            before = messageRepository.findGroupBefore(roomId, target.getId(), page);
            after = messageRepository.findGroupAfter(roomId, target.getId(), page);
        } else {
            kind = "DM";
            roomId = null;
            title = "Личная переписка";
            privacy = "Личная переписка, вас в ней нет";
            before = messageRepository.findPersonalBefore(
                    target.getSenderId(), target.getRecipientId(), target.getId(), page);
            after = messageRepository.findPersonalAfter(
                    target.getSenderId(), target.getRecipientId(), target.getId(), page);
        }
        List<ChatMessage> lines = new ArrayList<>();
        for (int i = before.size() - 1; i >= 0; i--) {
            lines.add(before.get(i));
        }
        lines.add(target);
        lines.addAll(after);
        List<AdminChatLineDto> dtoLines = lines.stream()
                .map(m -> AdminChatLineDto.builder()
                        .id(m.getId())
                        .authorId(m.getSenderId())
                        .authorName(((m.getSenderFirstName() == null ? "" : m.getSenderFirstName()) + " "
                                + (m.getSenderLastName() == null ? "" : m.getSenderLastName())).trim())
                        .text(m.isDeleted() ? "" : displayText(m.getContent()))
                        .timestamp(m.getTimestamp())
                        .deleted(m.isDeleted())
                        .highlighted(m.getId().equals(target.getId()))
                        .build())
                .toList();
        return AdminChatAroundDto.builder()
                .roomId(roomId)
                .chatKind(kind)
                .title(title)
                .highlightedId(target.getId())
                .privacy(privacy)
                .messages(dtoLines)
                .build();
    }

    @Transactional
    public void deleteMessage(Long messageId) {
        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Сообщение не найдено"));
        message.setDeleted(true);
        messageRepository.save(message);
        log.info("[Admin] Сообщение {} скрыто модерацией", messageId);
    }

    @Transactional
    public void applyBan(Long userId) {
        List<ChatRoom> rooms = chatRoomRepository.findAllRoomsByUserId(userId);
        for (ChatRoom room : rooms) {
            if (room.getOwnerId() != null && room.getOwnerId().equals(userId)) {
                continue;
            }
            chatParticipantRepository.findByChatIdAndUserId(room.getId(), userId)
                    .ifPresent(chatParticipantRepository::delete);
        }
        log.info("[Admin] Пользователь {} тихо выведен из чужих комнат", userId);
    }

    private static String displayText(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        return content
                .replaceAll("\\[SHARE_EVENT:\\d+\\]\\s*", "")
                .replaceAll("\\[SHARE_POST:\\d+\\]\\s*", "")
                .trim();
    }
}
