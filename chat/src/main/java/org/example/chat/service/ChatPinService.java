package org.example.chat.service;

import com.example.common.kafka.NotificationKafkaProducer;
import com.example.common.kafka.NotificationType;
import lombok.RequiredArgsConstructor;
import org.example.chat.entity.ChatMessage;
import org.example.chat.entity.ChatMessageDto;
import org.example.chat.entity.ChatParticipant;
import org.example.chat.entity.ChatPinnedMessage;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.PinnedMessageDto;
import org.example.chat.repository.ChatMessageRepository;
import org.example.chat.repository.ChatParticipantRepository;
import org.example.chat.repository.ChatPinnedMessageRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatPinService {
    private static final int MAX_PINS = 20;

    private final ChatPinnedMessageRepository pinRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatParticipantRepository participantRepository;
    private final ChatAccess chatAccess;
    private final ChatPreferenceService preferenceService;
    private final NotificationKafkaProducer notificationProducer;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<PinnedMessageDto> listRoomPins(Long chatId, Long userId) {
        chatAccess.requireParticipant(chatId, userId);
        return toDtos(pinRepository.findByChatIdOrderByPinnedAtDesc(chatId));
    }

    @Transactional(readOnly = true)
    public List<PinnedMessageDto> listPersonalPins(Long userId, Long partnerId) {
        return toDtos(pinRepository.findByDmUserMinAndDmUserMaxOrderByPinnedAtDesc(
                ChatAccess.dmMin(userId, partnerId), ChatAccess.dmMax(userId, partnerId)));
    }

    public Set<Long> pinnedIds(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Set.of();
        }
        return pinRepository.findByMessageIdIn(messageIds).stream()
                .map(ChatPinnedMessage::getMessageId)
                .collect(Collectors.toSet());
    }

    @Transactional
    public List<PinnedMessageDto> pinRoomMessage(Long chatId, Long messageId, Long userId, String firstName, String lastName, boolean notify) {
        ChatRoom room = chatAccess.requireRoom(chatId);
        chatAccess.assertCanPinMessage(room, userId);
        ChatMessage message = requireMessageInRoom(messageId, chatId);
        if (pinRepository.findByMessageId(messageId).isPresent()) {
            return listRoomPins(chatId, userId);
        }
        if (pinRepository.findByChatIdOrderByPinnedAtDesc(chatId).size() >= MAX_PINS) {
            throw new IllegalArgumentException("Можно закрепить не больше 20 сообщений в одном чате");
        }
        pinRepository.save(ChatPinnedMessage.builder()
                .messageId(messageId)
                .chatId(chatId)
                .pinnedBy(userId)
                .pinnedAt(LocalDateTime.now())
                .build());
        String actor = display(firstName, lastName);
        postPinSystem(chatId, actor + " закрепил(а) сообщение");
        if (notify) {
            notifyRoom(room, userId, firstName, lastName, "закрепил(а) сообщение");
        }
        return broadcastRoomPins(chatId);
    }

    @Transactional
    public List<PinnedMessageDto> unpinRoomMessage(Long chatId, Long messageId, Long userId) {
        ChatRoom room = chatAccess.requireRoom(chatId);
        chatAccess.assertCanPinMessage(room, userId);
        pinRepository.deleteByMessageId(messageId);
        return broadcastRoomPins(chatId);
    }

    @Transactional
    public List<PinnedMessageDto> pinPersonalMessage(Long userId, Long partnerId, Long messageId, String firstName, String lastName) {
        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Сообщение не найдено"));
        assertPersonalMessage(message, userId, partnerId);
        if (pinRepository.findByMessageId(messageId).isPresent()) {
            return listPersonalPins(userId, partnerId);
        }
        long min = ChatAccess.dmMin(userId, partnerId);
        long max = ChatAccess.dmMax(userId, partnerId);
        if (pinRepository.findByDmUserMinAndDmUserMaxOrderByPinnedAtDesc(min, max).size() >= MAX_PINS) {
            throw new IllegalArgumentException("Можно закрепить не больше 20 сообщений в одном чате");
        }
        pinRepository.save(ChatPinnedMessage.builder()
                .messageId(messageId)
                .dmUserMin(min)
                .dmUserMax(max)
                .pinnedBy(userId)
                .pinnedAt(LocalDateTime.now())
                .build());
        String actor = display(firstName, lastName);
        postPersonalPinSystem(userId, partnerId, actor + " закрепил(а) сообщение", firstName, lastName);
        return broadcastPersonalPins(userId, partnerId);
    }

    @Transactional
    public List<PinnedMessageDto> unpinPersonalMessage(Long userId, Long partnerId, Long messageId) {
        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Сообщение не найдено"));
        assertPersonalMessage(message, userId, partnerId);
        pinRepository.deleteByMessageId(messageId);
        return broadcastPersonalPins(userId, partnerId);
    }

    @Transactional
    public void removePinForDeletedMessage(Long messageId) {
        pinRepository.findByMessageId(messageId).ifPresent(pin -> {
            pinRepository.deleteByMessageId(messageId);
            if (pin.getChatId() != null) {
                broadcastRoomPins(pin.getChatId());
            } else if (pin.getDmUserMin() != null && pin.getDmUserMax() != null) {
                broadcastPersonalPins(pin.getDmUserMin(), pin.getDmUserMax());
            }
        });
    }

    private ChatMessage requireMessageInRoom(Long messageId, Long chatId) {
        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Сообщение не найдено"));
        if (message.isDeleted() || !chatId.equals(message.getChatId())) {
            throw new IllegalArgumentException("Это сообщение нельзя закрепить");
        }
        return message;
    }

    private void postPinSystem(Long chatId, String content) {
        ChatMessage saved = messageRepository.save(ChatMessage.builder()
                .chatId(chatId)
                .senderId(0L)
                .content(content)
                .timestamp(LocalDateTime.now())
                .isSystem(true)
                .build());
        messagingTemplate.convertAndSend("/topic/chat." + chatId, ChatMessageDto.builder()
                .id(saved.getId())
                .chatId(chatId)
                .senderId(0L)
                .content(content)
                .isSystem(true)
                .timestamp(saved.getTimestamp())
                .build());
    }

    private void postPersonalPinSystem(Long userId, Long partnerId, String content, String firstName, String lastName) {
        ChatMessage saved = messageRepository.save(ChatMessage.builder()
                .senderId(userId)
                .recipientId(partnerId)
                .senderFirstName(firstName)
                .senderLastName(lastName)
                .content(content)
                .timestamp(LocalDateTime.now())
                .isSystem(true)
                .read(true)
                .build());
        ChatMessageDto dto = ChatMessageDto.builder()
                .id(saved.getId())
                .senderId(userId)
                .recipientId(partnerId)
                .senderFirstName(firstName)
                .senderLastName(lastName)
                .content(content)
                .isSystem(true)
                .timestamp(saved.getTimestamp())
                .build();
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/messages", dto);
        messagingTemplate.convertAndSendToUser(String.valueOf(partnerId), "/queue/messages", dto);
    }

    private void assertPersonalMessage(ChatMessage message, Long userId, Long partnerId) {
        if (message.isDeleted() || message.getChatId() != null) {
            throw new IllegalArgumentException("Это сообщение нельзя закрепить");
        }
        Set<Long> pair = new HashSet<>();
        pair.add(message.getSenderId());
        pair.add(message.getRecipientId());
        if (!pair.contains(userId) || !pair.contains(partnerId)) {
            throw new SecurityException("Нет доступа к этому сообщению");
        }
    }

    private List<PinnedMessageDto> broadcastRoomPins(Long chatId) {
        List<PinnedMessageDto> pins = toDtos(pinRepository.findByChatIdOrderByPinnedAtDesc(chatId));
        Map<String, Object> payload = new HashMap<>();
        payload.put("pinUpdate", true);
        payload.put("chatId", chatId);
        payload.put("pins", pins);
        messagingTemplate.convertAndSend("/topic/chat." + chatId, payload);
        return pins;
    }

    private List<PinnedMessageDto> broadcastPersonalPins(Long userA, Long userB) {
        List<PinnedMessageDto> pins = listPersonalPins(userA, userB);
        Map<String, Object> payload = new HashMap<>();
        payload.put("pinUpdate", true);
        payload.put("partnerMin", ChatAccess.dmMin(userA, userB));
        payload.put("partnerMax", ChatAccess.dmMax(userA, userB));
        payload.put("pins", pins);
        messagingTemplate.convertAndSendToUser(String.valueOf(userA), "/queue/messages", payload);
        if (!userA.equals(userB)) {
            messagingTemplate.convertAndSendToUser(String.valueOf(userB), "/queue/messages", payload);
        }
        return pins;
    }

    private void notifyRoom(ChatRoom room, Long actorId, String firstName, String lastName, String text) {
        String label = "PIN:GROUP:" + room.getTitle();
        for (ChatParticipant participant : participantRepository.findAllByChatId(room.getId())) {
            if (participant.getUserId().equals(actorId)) {
                continue;
            }
            if (preferenceService.isMuted(participant.getUserId(), ChatAccess.roomScope(room.getId()))) {
                continue;
            }
            notificationProducer.sendEvent(
                    participant.getUserId(),
                    actorId,
                    firstName,
                    lastName,
                    NotificationType.NEW_CHAT_MESSAGE,
                    room.getId(),
                    null,
                    text,
                    label
            );
        }
    }

    private List<PinnedMessageDto> toDtos(List<ChatPinnedMessage> pins) {
        List<PinnedMessageDto> result = new ArrayList<>();
        for (ChatPinnedMessage pin : pins) {
            ChatMessage message = messageRepository.findById(pin.getMessageId()).orElse(null);
            if (message == null || message.isDeleted()) {
                continue;
            }
            String sender = display(message.getSenderFirstName(), message.getSenderLastName());
            result.add(PinnedMessageDto.builder()
                    .id(pin.getId())
                    .messageId(pin.getMessageId())
                    .chatId(pin.getChatId())
                    .content(message.getContent())
                    .photos(message.getPhotos() == null ? List.of() : new ArrayList<>(message.getPhotos()))
                    .senderName(sender)
                    .pinnedBy(pin.getPinnedBy())
                    .timestamp(message.getTimestamp())
                    .pinnedAt(pin.getPinnedAt())
                    .build());
        }
        return result;
    }

    private static String display(String firstName, String lastName) {
        String first = firstName == null || firstName.isBlank() ? "Участник" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        return (first + " " + last).trim();
    }
}
