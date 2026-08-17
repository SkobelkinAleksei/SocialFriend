package org.example.chat.service;

import com.example.common.RequestData;
import com.example.common.dto.event.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chat.client.FriendCheckClient;
import org.example.chat.entity.*;
import org.example.chat.repository.ChatRoomRepository;
import org.example.chat.repository.ChatParticipantRepository;
import org.example.chat.repository.ChatMessageRepository;
import org.example.chat.repository.ChatPinnedMessageRepository;
import org.example.chat.repository.ChatPollRepository;
import org.example.chat.repository.ChatPollVoteRepository;
import org.example.restclient.config.IHttpCore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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
@Slf4j
public class GroupChatServiceImpl implements GroupChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService messageService;
    private final ChatPollService pollService;
    private final ChatPollRepository pollRepository;
    private final ChatPollVoteRepository voteRepository;
    private final FriendCheckClient friendCheckClient;
    private final IHttpCore httpCore;
    private final ChatAccess chatAccess;
    private final ChatPreferenceService preferenceService;

    @PersistenceContext
    private EntityManager entityManager;
    private final ChatPinnedMessageRepository pinnedMessageRepository;

    @Value("${app.services.user-base-url:http://localhost:8081}")
    private String userBaseUrl;

    @Value("${app.services.event-base-url:http://localhost:8088}")
    private String eventBaseUrl;

    @Override
    @Transactional
    public Long createGroupRoom(Long eventId, String title, Long ownerId) {
        log.info("Создание группового чата для события ID: {}, Название: {}", eventId, title);
        if (eventId == null) {
            throw new IllegalArgumentException("eventId обязателен для чата события");
        }

        ChatRoom existing = chatRoomRepository.findByEventId(eventId).orElse(null);
        if (existing != null) {
            log.info("Чат для события {} уже существует, chatId={}", eventId, existing.getId());
            return existing.getId();
        }

        ChatRoom room = ChatRoom.builder()
                .eventId(eventId)
                .title(title)
                .ownerId(ownerId)
                .createdAt(LocalDateTime.now())
                .roomType(ChatRoomType.EVENT)
                .addMembersPolicy(ChatRoomPolicy.OWNER_ONLY)
                .renamePolicy(ChatRoomPolicy.OWNER_ONLY)
                .build();
        ChatRoom savedRoom = chatRoomRepository.save(room);

        ChatParticipant participant = ChatParticipant.builder()
                .chat(savedRoom)
                .userId(ownerId)
                .unreadCount(1)
                .build();
        chatParticipantRepository.save(participant);

        String systemText = "Групповой чат для события создан";
        ChatMessage systemMsg = ChatMessage.builder()
                .chatId(savedRoom.getId())
                .senderId(0L)
                .content(systemText)
                .timestamp(LocalDateTime.now())
                .isSystem(true)
                .build();

        ChatMessage savedMsg = messageRepository.save(systemMsg);
        messagingTemplate.convertAndSend("/topic/chat." + savedRoom.getId(), savedMsg);
        log.info("[WebSocket] Стартовое системное сообщение отправлено в топик: /topic/chat.{}", savedRoom.getId());

        return savedRoom.getId();
    }

    @Override
    @Transactional
    public void deleteGroupMessagesBatch(List<Long> messageIds, Long senderId) {
        if (messageIds == null || messageIds.isEmpty()) return;

        for (Long id : messageIds) {
            ChatMessage message = messageRepository.findById(id).orElse(null);
            if (message != null && message.getChatId() != null && message.getSenderId().equals(senderId)) {
                message.setDeleted(true);
                messageRepository.save(message);
            }
        }
    }

    @Override
    @Transactional
    public void joinToGroupRoom(Long eventId, Long userId) {
        log.info("Добавление пользователя ID: {} в чат события ID: {}", userId, eventId);

        ChatRoom room = chatRoomRepository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Чат для события не найден"));

        if (chatParticipantRepository.findByChatIdAndUserId(room.getId(), userId).isEmpty()) {
            try {
                ChatParticipant participant = ChatParticipant.builder()
                        .chat(room)
                        .userId(userId)
                        .build();
                chatParticipantRepository.save(participant);
            } catch (DataIntegrityViolationException ignored) {
                log.info("Пользователь {} уже в чате события {}", userId, eventId);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomDto> getMyGroupRooms(Long userId) {
        List<ChatRoom> rooms = chatRoomRepository.findAllRoomsByUserIdSortedByLatestMessage(userId);
        List<ChatRoomDto> enrichedRooms = new java.util.ArrayList<>();
        List<String> scopeKeys = rooms.stream().map(room -> ChatAccess.roomScope(room.getId())).toList();
        java.util.Map<String, org.example.chat.entity.ChatUserPref> prefs = preferenceService.mapForUser(userId, scopeKeys);

        for (ChatRoom room : rooms) {
            int unreadCount = chatParticipantRepository.findByChatIdAndUserId(room.getId(), userId)
                    .map(ChatParticipant::getUnreadCount)
                    .orElse(0);

            List<ChatMessage> history = messageRepository.findGroupChatHistory(room.getId());
            if (history == null) history = new java.util.ArrayList<>();

            int cutIndex = 0;
            String targetUserMarker = String.format("[GROUP_CHAT_HISTORY_CLEARED:%d]", userId);

            for (int i = history.size() - 1; i >= 0; i--) {
                if (targetUserMarker.equals(history.get(i).getContent())) {
                    cutIndex = i + 1;
                    break;
                }
            }

            List<ChatMessage> visibleHistory = history.subList(cutIndex, history.size());

            ChatRoomDto.ChatRoomDtoBuilder dtoBuilder = ChatRoomDto.builder()
                    .id(room.getId())
                    .eventId(room.getEventId())
                    .title(room.getTitle())
                    .ownerId(room.getOwnerId())
                    .createdAt(room.getCreatedAt())
                    .unread(unreadCount)
                    .roomType(room.getRoomType() != null ? room.getRoomType() : ChatRoomType.EVENT)
                    .addMembersPolicy(room.getAddMembersPolicy() != null ? room.getAddMembersPolicy() : ChatRoomPolicy.OWNER_ONLY)
                    .renamePolicy(room.getRenamePolicy() != null ? room.getRenamePolicy() : ChatRoomPolicy.OWNER_ONLY)
                    .avatarUrl(room.getAvatarUrl())
                    .avatarPolicy(room.getAvatarPolicy() != null ? room.getAvatarPolicy() : ChatRoomPolicy.OWNER_ONLY)
                    .admin(chatAccess.isAdmin(room, userId));
            org.example.chat.entity.ChatUserPref pref = prefs.get(ChatAccess.roomScope(room.getId()));
            if (pref != null) {
                dtoBuilder.muted(pref.isMuted());
                dtoBuilder.pinned(pref.getPinnedAt() != null);
                dtoBuilder.pinnedAt(pref.getPinnedAt());
            }

            if (!visibleHistory.isEmpty()) {
                ChatMessage lastMsg = null;

                for (int i = visibleHistory.size() - 1; i >= 0; i--) {
                    ChatMessage msg = visibleHistory.get(i);
                    if (!msg.isDeleted() && (msg.getContent() == null || !msg.getContent().startsWith("[GROUP_CHAT_HISTORY_CLEARED:"))) {
                        lastMsg = msg;
                        break;
                    }
                }

                boolean allGroupMessagesDeleted = false;
                if (lastMsg == null) {
                    lastMsg = visibleHistory.get(visibleHistory.size() - 1);
                    allGroupMessagesDeleted = true;
                }

                boolean isEventCanceled = visibleHistory.stream()
                        .anyMatch(m -> "Встреча отменена, чат будет удален".equals(m.getContent()));

                String groupContentText;
                if (isEventCanceled) {
                    groupContentText = "Встреча отменена, чат будет удален";
                } else if (lastMsg.getContent() != null && lastMsg.getContent().startsWith("[GROUP_CHAT_HISTORY_CLEARED:")) {
                    groupContentText = "Переписка создана";
                } else {
                    groupContentText = allGroupMessagesDeleted ? "Сообщение удалено" : ChatMessageServiceImpl.sidebarPreview(lastMsg);
                }
                if (groupContentText != null && groupContentText.contains("[KICKED_ID:")) {
                    groupContentText = groupContentText.replaceAll("\\s*\\[KICKED_ID:\\d+\\]", "").trim();
                }
                if (groupContentText != null && groupContentText.contains("[LEFT_ID:")) {
                    groupContentText = groupContentText.replaceAll("\\s*\\[LEFT_ID:\\d+\\]", "").trim();
                }
                if (groupContentText != null && groupContentText.contains("[JOINED_ID:")) {
                    groupContentText = groupContentText.replaceAll("\\s*\\[JOINED_ID:\\d+\\]", "").trim();
                }

                dtoBuilder.lastMessage(groupContentText);

                if (lastMsg.getTimestamp() != null) {
                    String formattedTime = lastMsg.getTimestamp().toLocalTime()
                            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

                    if (isEventCanceled) {
                        ChatMessage cancelMessage = visibleHistory.stream()
                                .filter(m -> "Встреча отменена, чат будет удален".equals(m.getContent()))
                                .findFirst().orElse(lastMsg);

                        dtoBuilder.time(cancelMessage.getTimestamp().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
                        dtoBuilder.createdAt(cancelMessage.getTimestamp());
                    } else if (lastMsg.getContent() != null && lastMsg.getContent().startsWith("[GROUP_CHAT_HISTORY_CLEARED:")) {
                        dtoBuilder.time("");
                        dtoBuilder.createdAt(room.getCreatedAt());
                    } else {
                        dtoBuilder.time(formattedTime);
                        dtoBuilder.createdAt(lastMsg.getTimestamp());
                    }
                }
            } else {
                dtoBuilder.lastMessage("Переписка создана");
                dtoBuilder.time("");
                if (cutIndex > 0) {
                    dtoBuilder.createdAt(history.get(cutIndex - 1).getTimestamp());
                } else {
                    dtoBuilder.createdAt(room.getCreatedAt());
                }
            }
            enrichedRooms.add(dtoBuilder.build());
        }

        return enrichedRooms;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getGroupChatHistory(Long chatId, Long userId) {
        if (userId == null || chatParticipantRepository.findByChatIdAndUserId(chatId, userId).isEmpty()) {
            throw new SecurityException("Вы не участник этого группового чата");
        }

        List<ChatMessage> history = messageRepository.findGroupChatHistory(chatId);
        if (history == null) return List.of();

        int cutIndex = 0;
        String targetUserMarker = String.format("[GROUP_CHAT_HISTORY_CLEARED:%d]", userId);

        for (int i = history.size() - 1; i >= 0; i--) {
            if (targetUserMarker.equals(history.get(i).getContent())) {
                cutIndex = i + 1;
                break;
            }
        }

        List<ChatMessage> visibleHistory = history.subList(cutIndex, history.size());

        List<Long> ids = visibleHistory.stream()
                .filter(msg -> msg.getContent() == null || !msg.getContent().contains("[GROUP_CHAT_HISTORY_CLEARED:"))
                .map(ChatMessage::getId)
                .toList();
        List<ChatMessageDto> dtos = messageService.convertToDtoList(ids);
        java.util.Map<Long, org.example.chat.entity.ChatPollDto> polls = pollService.mapByMessageIds(ids, userId);
        for (ChatMessageDto dto : dtos) {
            if (dto.getId() != null && polls.containsKey(dto.getId())) {
                dto.setPoll(polls.get(dto.getId()));
            }
        }
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getGroupChatHistoryPage(Long chatId, Long userId, Long beforeId, int size) {
        if (userId == null || chatParticipantRepository.findByChatIdAndUserId(chatId, userId).isEmpty()) {
            throw new SecurityException("Вы не участник этого группового чата");
        }
        int pageSize = Math.min(Math.max(size, 1), 100);
        Long minId = messageRepository.findGroupClearCutId(chatId, userId);
        if (minId == null) minId = 0L;
        Long before = beforeId == null ? Long.MAX_VALUE : beforeId;
        List<Long> idsDesc = messageRepository.findGroupHistoryPageIds(
                chatId, minId, before, org.springframework.data.domain.PageRequest.of(0, pageSize));
        if (idsDesc == null || idsDesc.isEmpty()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>(idsDesc);
        java.util.Collections.reverse(ids);
        List<ChatMessageDto> dtos = messageService.convertToDtoList(ids);
        java.util.Map<Long, org.example.chat.entity.ChatPollDto> polls = pollService.mapByMessageIds(ids, userId);
        for (ChatMessageDto dto : dtos) {
            if (dto.getId() != null && polls.containsKey(dto.getId())) {
                dto.setPoll(polls.get(dto.getId()));
            }
        }
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public ChatPhotoPageDto getGroupChatPhotos(Long chatId, Long userId, int page, int size) {
        List<ChatMessageDto> history = getGroupChatHistory(chatId, userId);
        return ChatMessageServiceImpl.paginatePhotoItems(
                ChatMessageServiceImpl.collectPhotoItems(history), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatPhotoPageDto getGroupChatFiles(Long chatId, Long userId, int page, int size) {
        List<ChatMessageDto> history = getGroupChatHistory(chatId, userId);
        return ChatMessageServiceImpl.paginatePhotoItems(
                ChatMessageServiceImpl.collectFileItems(history), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatPhotoPageDto getGroupChatVoices(Long chatId, Long userId, int page, int size) {
        List<ChatMessageDto> history = getGroupChatHistory(chatId, userId);
        return ChatMessageServiceImpl.paginatePhotoItems(
                ChatMessageServiceImpl.collectVoiceItems(history), page, size);
    }

    @Override
    @Transactional
    public void joinToGroupRoomAndNotify(Long eventId, Long userId, String firstName, String lastName) {
        log.info("Добавление пользователя {} {} в чат события {}", firstName, lastName, eventId);

        ChatRoom room = chatRoomRepository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Чат для события не найден"));

        if (chatParticipantRepository.findByChatIdAndUserId(room.getId(), userId).isPresent()) {
            log.info("Пользователь {} уже в чате события {}, повторное уведомление не шлём", userId, eventId);
            return;
        }

        joinToGroupRoom(eventId, userId);

        String safeFirst = (firstName == null || firstName.isBlank()) ? "Участник" : firstName.trim();
        String safeLast = lastName == null ? "" : lastName.trim();
        String fullName = (safeFirst + " " + safeLast).trim();
        String systemText = fullName + " присоединился(ась) к событию [JOINED_ID:" + userId + "]";
        ChatMessage systemMsg = ChatMessage.builder()
                .chatId(room.getId())
                .senderId(0L)
                .content(systemText)
                .timestamp(LocalDateTime.now())
                .isSystem(true)
                .build();

        ChatMessage saved = messageRepository.save(systemMsg);
        chatParticipantRepository.incrementUnreadCountForAndAllByChatId(room.getId());

        chatParticipantRepository.findByChatIdAndUserId(room.getId(), userId).ifPresent(p -> {
            if (p.getUnreadCount() == 0) {
                p.setUnreadCount(1);
                chatParticipantRepository.save(p);
            }
        });

        messagingTemplate.convertAndSend("/topic/chat." + room.getId(), saved);
        log.info("[WebSocket] Системное сообщение отправлено в топик: /topic/chat.{}", room.getId());

        // Личный сигнал новичку — FE подтянет комнату в список без F5
        java.util.Map<String, Object> joinedSignal = new java.util.HashMap<>();
        joinedSignal.put("content", "[GROUP_CHAT_JOINED]");
        joinedSignal.put("chatId", room.getId());
        joinedSignal.put("eventId", eventId);
        joinedSignal.put("chatTitle", room.getTitle());
        joinedSignal.put("isSystem", true);
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/messages",
                joinedSignal
        );
    }

    @Override
    @Transactional
    public void resetUnreadCount(Long chatId, Long userId) {
        log.info("[Chat-Service] Сброс счетчика непрочитанных для пользователя ID: {} в чате ID: {}", userId, chatId);

        ChatParticipant participant = chatParticipantRepository.findByChatIdAndUserId(chatId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Участник с userId %d не найден в чате %d", userId, chatId)
                ));
        participant.setUnreadCount(0);
        chatParticipantRepository.save(participant);

        log.info("[Chat-Service - SUCCESS] Счётчик непрочитанных сброшен только для userId={}", userId);
    }

    @Override
    @Transactional
    public void leaveGroupRoom(Long eventId, Long userId, String firstName, String lastName) {
        log.info("[Chat-Service] Получен запрос на аннулирование связи пользователя {} в событии {}", userId, eventId);

        ChatRoom room = chatRoomRepository.findByEventId(eventId).orElse(null);
        if (room != null) {
            boolean isRealChatParticipant = chatParticipantRepository
                    .findByChatIdAndUserId(room.getId(), userId).isPresent();

            if (isRealChatParticipant) {
                String safeFirst = (firstName == null || firstName.isBlank()) ? "Участник" : firstName.trim();
                String safeLast = lastName == null ? "" : lastName.trim();
                String fullName = (safeFirst + " " + safeLast).trim();
                String systemText = fullName + " покинул(а) событие [LEFT_ID:" + userId + "]";
                ChatMessage leaveMsg = ChatMessage.builder()
                        .chatId(room.getId())
                        .senderId(0L)
                        .content(systemText)
                        .timestamp(LocalDateTime.now())
                        .isSystem(true)
                        .build();
                ChatMessage savedMsg = messageRepository.save(leaveMsg);
                messagingTemplate.convertAndSend("/topic/chat." + room.getId(), savedMsg);
                log.info("[Chat-Service] Системное уведомление о выходе пользователя {} отправлено в чат", userId);
            } else {
                log.info("[Chat-Service] Пользователь {} не был участником чата. Ложное уведомление 'покинул чат' заблокировано.", userId);
            }
        }

        chatParticipantRepository.deleteByEventIdAndUserId(eventId, userId);
        log.info("[Chat-Service] Связь пользователя {} удалена из участников чата встречи", userId);
    }

    @Override
    @Transactional
    public void clearGroupChatForUser(Long eventId, Long userId) {
        ChatRoom room = chatRoomRepository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Чат встречи не найден"));

        log.info("[Group-Chat-Service] Запись маркера массовой очистки группы ID: {} для пользователя {}", room.getId(), userId);

        ChatMessage clearMarker = ChatMessage.builder()
                .chatId(room.getId())
                .senderId(0L)
                .content(String.format("[GROUP_CHAT_HISTORY_CLEARED:%d]", userId))
                .timestamp(LocalDateTime.now())
                .isSystem(true)
                .read(true)
                .build();
        messageRepository.save(clearMarker);
    }

    @Override
    @Transactional
    public void cancelAndNotifyGroupRoom(Long eventId, Long ownerId) {
        ChatRoom room = chatRoomRepository.findByEventId(eventId).orElse(null);
        if (room == null) {
            return;
        }
        if (room.getOwnerId() == null || !room.getOwnerId().equals(ownerId)) {
            throw new SecurityException("Отменить чат встречи может только организатор");
        }

        log.info("[Chat-Service] Владелец {} инициировал отмену встречи {}. Запись маркера отмены.", ownerId, eventId);

        postSystem(room, ChatSystemMessages.EVENT_CANCELED);
    }

    @Override
    @Transactional
    public void kickParticipantFromChatRoom(Long eventId, Long userId, String firstName, String lastName) {
        log.info("[Chat-Service] Принудительное удаление связи пользователя {} в чате события {}", userId, eventId);

        ChatRoom room = chatRoomRepository.findByEventId(eventId).orElse(null);
        if (room != null) {
            boolean isRealChatParticipant = chatParticipantRepository
                    .findByChatIdAndUserId(room.getId(), userId).isPresent();

            if (isRealChatParticipant) {
                String safeFirst = (firstName == null || firstName.isBlank()) ? "Участник" : firstName.trim();
                String safeLast = lastName == null ? "" : lastName.trim();
                String fullName = (safeFirst + " " + safeLast).trim();
                String systemText = fullName + " был исключен организатором из встречи [KICKED_ID:" + userId + "]";
                ChatMessage kickMsg = ChatMessage.builder()
                        .chatId(room.getId())
                        .senderId(0L)
                        .content(systemText)
                        .timestamp(LocalDateTime.now())
                        .isSystem(true)
                        .build();

                ChatMessage savedMsg = messageRepository.save(kickMsg);
                messagingTemplate.convertAndSend("/topic/chat." + room.getId(), savedMsg);
                log.info("[Chat-Service] Системное уведомление об исключении пользователя {} отправлено в чат", userId);
            }
        }

        chatParticipantRepository.deleteByEventIdAndUserId(eventId, userId);
        log.info("[Chat-Service - KICK SUCCESS] Пользователь {} успешно стерт из участников чата встречи {}", userId, eventId);
    }

    @Override
    @Transactional
    public void postSystemMessageByEventId(Long eventId, String content) {
        ChatRoom room = chatRoomRepository.findByEventId(eventId).orElse(null);
        if (room == null || content == null || content.isBlank()) {
            return;
        }
        if (messageRepository.existsByChatIdAndContent(room.getId(), content)) {
            return;
        }
        postSystem(room, content);
    }

    @Override
    @Transactional
    public void createKeepChatPollByEventId(Long eventId, com.example.common.dto.scheduler.KeepChatPollRequest request) {
        ChatRoom room = chatRoomRepository.findByEventId(eventId).orElse(null);
        if (room == null) {
            return;
        }
        ChatMessageDto dto = pollService.createKeepChatPoll(room.getId(), request);
        messagingTemplate.convertAndSend("/topic/chat." + room.getId(), dto);
    }

    @Override
    @Transactional
    public boolean resolveKeepChatPollByEventId(Long eventId) {
        ChatRoom room = chatRoomRepository.findByEventId(eventId).orElse(null);
        if (room == null) {
            return true;
        }
        return pollService.resolveKeepChatPoll(room.getId());
    }

    @Override
    @Transactional
    public void closeRoomByEventId(Long eventId) {
        ChatRoom room = chatRoomRepository.findByEventId(eventId).orElse(null);
        if (room == null) {
            return;
        }
        if (!messageRepository.existsByChatIdAndContent(room.getId(), ChatSystemMessages.CHAT_CLOSING_FEW_PEOPLE)) {
            postSystem(room, ChatSystemMessages.CHAT_CLOSING_FEW_PEOPLE);
        }
        deleteRoom(room, "FEW_PEOPLE");
    }

    @Override
    @Transactional
    public void deleteRoomByEventId(Long eventId) {
        ChatRoom room = chatRoomRepository.findByEventId(eventId).orElse(null);
        if (room == null) {
            return;
        }
        deleteRoom(room, "CANCELLED");
    }

    private void postSystem(ChatRoom room, String content) {
        postSystem(room, content, List.of());
    }

    private void postSystem(ChatRoom room, String content, List<String> photos) {
        ChatMessage systemMsg = ChatMessage.builder()
                .chatId(room.getId())
                .senderId(0L)
                .content(content)
                .timestamp(LocalDateTime.now())
                .isSystem(true)
                .photos(photos == null ? new ArrayList<>() : new ArrayList<>(photos))
                .build();
        ChatMessage saved = messageRepository.save(systemMsg);
        messageRepository.flush();
        chatParticipantRepository.incrementUnreadCountForAndAllByChatId(room.getId());
        messagingTemplate.convertAndSend("/topic/chat." + room.getId(), messageService.convertToDto(saved.getId()));
    }

    private void deleteRoom(ChatRoom room, String reason) {
        ChatMessageDto deleted = ChatMessageDto.builder()
                .chatId(room.getId())
                .senderId(0L)
                .content(ChatSystemMessages.ROOM_DELETED)
                .isSystem(true)
                .roomDeleted(true)
                .timestamp(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSend("/topic/chat." + room.getId(), deleted);
        log.info("[Chat-Service] Удаление комнаты {} встречи {}, причина {}", room.getId(), room.getEventId(), reason);

        List<ChatPoll> polls = pollRepository.findByChatId(room.getId());
        for (ChatPoll poll : polls) {
            voteRepository.deleteByPollId(poll.getId());
        }
        pollRepository.deleteAll(polls);

        List<ChatMessage> messages = messageRepository.findByChatId(room.getId());
        for (ChatMessage message : messages) {
            if (message.getReplies() != null) {
                message.getReplies().clear();
            }
            if (message.getBundledForwards() != null) {
                message.getBundledForwards().clear();
            }
            message.setForwardedFrom(null);
        }
        messageRepository.saveAll(messages);
        messageRepository.flush();
        messageRepository.deleteAll(messages);

        pinnedMessageRepository.deleteByChatId(room.getId());
        chatParticipantRepository.deleteByChatId(room.getId());
        preferenceService.deleteRoomPrefs(room.getId());
        entityManager.flush();
        entityManager.clear();
        chatRoomRepository.deleteById(room.getId());
    }

    @Override
    @Transactional
    public ChatRoomDto createPersonalGroup(Long ownerId, String firstName, String lastName, CreatePersonalGroupRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Укажите название группового чата");
        }
        String title = request.getTitle().trim();
        if (title.length() > 80) {
            throw new IllegalArgumentException("Название слишком длинное");
        }
        Set<Long> memberIds = new HashSet<>();
        if (request.getMemberIds() != null) {
            for (Long id : request.getMemberIds()) {
                if (id != null && !id.equals(ownerId)) {
                    memberIds.add(id);
                }
            }
        }
        if (memberIds.isEmpty()) {
            throw new IllegalArgumentException("Добавьте хотя бы одного друга");
        }
        for (Long memberId : memberIds) {
            if (!friendCheckClient.areFriends(ownerId, memberId)) {
                throw new IllegalArgumentException("В групповой чат можно добавить только друзей");
            }
        }

        ChatRoom room = ChatRoom.builder()
                .eventId(null)
                .title(title)
                .ownerId(ownerId)
                .createdAt(LocalDateTime.now())
                .roomType(ChatRoomType.PERSONAL_GROUP)
                .addMembersPolicy(request.getAddMembersPolicy() != null ? request.getAddMembersPolicy() : ChatRoomPolicy.OWNER_ONLY)
                .renamePolicy(request.getRenamePolicy() != null ? request.getRenamePolicy() : ChatRoomPolicy.OWNER_ONLY)
                .avatarPolicy(request.getAvatarPolicy() != null ? request.getAvatarPolicy() : ChatRoomPolicy.OWNER_ONLY)
                .avatarUrl(blankToNull(request.getAvatarUrl()))
                .build();
        ChatRoom savedRoom = chatRoomRepository.save(room);

        List<Long> allMembers = new ArrayList<>();
        allMembers.add(ownerId);
        allMembers.addAll(memberIds);
        for (Long userId : allMembers) {
            chatParticipantRepository.save(ChatParticipant.builder()
                    .chat(savedRoom)
                    .userId(userId)
                    .unreadCount(userId.equals(ownerId) ? 0 : 1)
                    .build());
        }

        String ownerName = displayName(firstName, lastName);
        postSystem(savedRoom, "Групповой чат создан");
        for (Long memberId : memberIds) {
            UserDto profile = fetchUsers(Set.of(memberId)).get(memberId);
            String name = profile != null ? displayName(profile.getFirstName(), profile.getLastName()) : "Участник";
            postSystem(savedRoom, ownerName + " добавил(а) " + name + " в чат [JOINED_ID:" + memberId + "]");
            sendJoinedSignal(savedRoom, memberId);
        }

        log.info("[Chat] Личный групповой чат {} создан пользователем {}", savedRoom.getId(), ownerId);
        return getMyGroupRooms(ownerId).stream()
                .filter(dto -> dto.getId().equals(savedRoom.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Не удалось создать групповой чат"));
    }

    @Override
    @Transactional
    public ChatRoomDto updatePersonalGroup(Long chatId, Long userId, String firstName, String lastName, UpdatePersonalGroupRequest request) {
        ChatRoom room = requirePersonalGroup(chatId, userId);
        if (request == null) {
            throw new IllegalArgumentException("Пустой запрос");
        }
        boolean privileged = chatAccess.isAdmin(room, userId);
        if (request.getTitle() != null) {
            boolean canRename = privileged || room.getRenamePolicy() == ChatRoomPolicy.EVERYONE;
            if (!canRename) {
                throw new SecurityException("Менять название может только создатель чата");
            }
            String title = request.getTitle().trim();
            if (title.isBlank()) {
                throw new IllegalArgumentException("Название не может быть пустым");
            }
            if (title.length() > 80) {
                throw new IllegalArgumentException("Название слишком длинное");
            }
            if (!title.equals(room.getTitle())) {
                room.setTitle(title);
                postSystem(room, displayName(firstName, lastName) + " изменил(а) название чата: " + title);
            }
        }
        if (request.getAvatarUrl() != null) {
            boolean canChangeAvatar = privileged || room.getAvatarPolicy() == ChatRoomPolicy.EVERYONE;
            if (!canChangeAvatar) {
                throw new SecurityException("Менять аватар может только создатель чата");
            }
            String nextAvatar = blankToNull(request.getAvatarUrl());
            if (!java.util.Objects.equals(nextAvatar, room.getAvatarUrl())) {
                room.setAvatarUrl(nextAvatar);
                String name = displayName(firstName, lastName);
                if (nextAvatar != null) {
                    postSystem(room, name + " сменил(а) аватарку группы [AVATAR_ID:" + userId + "]", List.of(nextAvatar));
                } else {
                    postSystem(room, name + " убрал(а) аватарку группы [AVATAR_ID:" + userId + "]");
                }
            }
        }
        if (request.getAddMembersPolicy() != null || request.getRenamePolicy() != null || request.getAvatarPolicy() != null) {
            if (!privileged) {
                throw new SecurityException("Настройки чата может менять только администратор");
            }
            boolean settingsChanged = false;
            if (request.getAddMembersPolicy() != null && request.getAddMembersPolicy() != room.getAddMembersPolicy()) {
                room.setAddMembersPolicy(request.getAddMembersPolicy());
                settingsChanged = true;
            }
            if (request.getRenamePolicy() != null && request.getRenamePolicy() != room.getRenamePolicy()) {
                room.setRenamePolicy(request.getRenamePolicy());
                settingsChanged = true;
            }
            if (request.getAvatarPolicy() != null && request.getAvatarPolicy() != room.getAvatarPolicy()) {
                room.setAvatarPolicy(request.getAvatarPolicy());
                settingsChanged = true;
            }
            if (settingsChanged) {
                postSystem(room, displayName(firstName, lastName) + " изменил(а) настройки чата");
            }
        }
        chatRoomRepository.save(room);
        return getMyGroupRooms(userId).stream()
                .filter(dto -> dto.getId().equals(chatId))
                .findFirst()
                .orElseThrow();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMemberDto> listMembers(Long chatId, Long userId) {
        requireParticipant(chatId, userId);
        ChatRoom room = chatRoomRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Чат не найден"));
        List<ChatParticipant> participants = chatParticipantRepository.findAllByChatId(chatId);
        Set<Long> ids = participants.stream().map(ChatParticipant::getUserId).collect(Collectors.toSet());
        Map<Long, UserDto> users = fetchUsers(ids);
        List<ChatMemberDto> result = new ArrayList<>();
        for (ChatParticipant participant : participants) {
            UserDto profile = users.get(participant.getUserId());
            result.add(ChatMemberDto.builder()
                    .userId(participant.getUserId())
                    .firstName(profile != null ? profile.getFirstName() : "Участник")
                    .lastName(profile != null ? profile.getLastName() : "")
                    .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                    .owner(room.getOwnerId().equals(participant.getUserId()))
                    .admin(room.getOwnerId().equals(participant.getUserId()) || participant.isAdmin())
                    .build());
        }
        result.sort((a, b) -> {
            if (a.isOwner() != b.isOwner()) {
                return a.isOwner() ? -1 : 1;
            }
            if (a.isAdmin() != b.isAdmin()) {
                return a.isAdmin() ? -1 : 1;
            }
            return String.valueOf(a.getFirstName()).compareToIgnoreCase(String.valueOf(b.getFirstName()));
        });
        return result;
    }

    @Override
    @Transactional
    public void addMembers(Long chatId, Long actorId, String firstName, String lastName, AddGroupMembersRequest request) {
        ChatRoom room = requirePersonalGroup(chatId, actorId);
        boolean canAdd = chatAccess.isAdmin(room, actorId) || room.getAddMembersPolicy() == ChatRoomPolicy.EVERYONE;
        if (!canAdd) {
            throw new SecurityException("Добавлять участников может только создатель чата");
        }
        if (request == null || request.getMemberIds() == null || request.getMemberIds().isEmpty()) {
            throw new IllegalArgumentException("Выберите друзей для приглашения");
        }
        for (Long memberId : request.getMemberIds()) {
            if (memberId == null || memberId.equals(actorId)) {
                continue;
            }
            if (chatParticipantRepository.findByChatIdAndUserId(chatId, memberId).isPresent()) {
                continue;
            }
            if (!friendCheckClient.areFriends(actorId, memberId)) {
                throw new IllegalArgumentException("В групповой чат можно добавить только друзей");
            }
            chatParticipantRepository.save(ChatParticipant.builder()
                    .chat(room)
                    .userId(memberId)
                    .unreadCount(1)
                    .build());
            UserDto profile = fetchUsers(Set.of(memberId)).get(memberId);
            String name = profile != null ? displayName(profile.getFirstName(), profile.getLastName()) : "Участник";
            postSystem(room, displayName(firstName, lastName) + " добавил(а) " + name + " в чат [JOINED_ID:" + memberId + "]");
            sendJoinedSignal(room, memberId);
        }
    }

    @Override
    @Transactional
    public void leavePersonalGroup(Long chatId, Long userId, String firstName, String lastName) {
        ChatRoom room = requirePersonalGroup(chatId, userId);
        if (room.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("Сначала передайте чат другому участнику или удалите его");
        }
        String name = displayName(firstName, lastName);
        postSystem(room, name + " покинул(а) чат [LEFT_ID:" + userId + "]");
        chatParticipantRepository.findByChatIdAndUserId(chatId, userId)
                .ifPresent(chatParticipantRepository::delete);

        List<ChatParticipant> remaining = chatParticipantRepository.findAllByChatId(chatId);
        if (remaining.isEmpty()) {
            deleteRoom(room, "PERSONAL_GROUP_EMPTY");
        }
    }

    @Override
    @Transactional
    public void kickPersonalGroupMember(Long chatId, Long actorId, Long targetUserId, String firstName, String lastName) {
        kickGroupMember(chatId, actorId, targetUserId, firstName, lastName);
    }

    @Override
    @Transactional
    public void kickGroupMember(Long chatId, Long actorId, Long targetUserId, String firstName, String lastName) {
        ChatRoom room = chatAccess.requireRoom(chatId);
        chatAccess.assertCanKick(room, actorId, targetUserId);
        if (room.getRoomType() == ChatRoomType.EVENT && room.getEventId() != null) {
            try {
                httpCore.post(
                        eventBaseUrl + "/api/v1/internal/events/" + room.getEventId() + "/kick/" + targetUserId,
                        org.springframework.http.HttpMethod.DELETE,
                        org.springframework.http.HttpEntity.EMPTY,
                        Void.class
                );
                return;
            } catch (Exception e) {
                log.warn("[Chat] Не удалось кикнуть через event-service, удаляем из чата локально: {}", e.getMessage());
            }
        }
        removeMemberLocally(room, targetUserId, firstName, lastName);
    }

    @Override
    @Transactional
    public void setMemberAdmin(Long chatId, Long actorId, Long targetUserId, boolean admin, String firstName, String lastName) {
        ChatRoom room = chatAccess.requireRoom(chatId);
        chatAccess.requireParticipant(chatId, actorId);
        chatAccess.assertOwner(room, actorId);
        if (actorId.equals(targetUserId) || room.getOwnerId().equals(targetUserId)) {
            throw new IllegalArgumentException("Создатель чата и так главный");
        }
        ChatParticipant target = chatAccess.requireParticipant(chatId, targetUserId);
        target.setAdmin(admin);
        chatParticipantRepository.save(target);
        UserDto profile = fetchUsers(Set.of(targetUserId)).get(targetUserId);
        String name = profile != null ? displayName(profile.getFirstName(), profile.getLastName()) : "Участник";
        String actor = displayName(firstName, lastName);
        postSystem(room, admin
                ? actor + " назначил(а) " + name + " администратором"
                : actor + " снял(а) " + name + " с роли администратора");
    }

    @Override
    @Transactional
    public ChatRoomDto transferPersonalGroup(Long chatId, Long ownerId, Long newOwnerId, String firstName, String lastName) {
        ChatRoom room = requirePersonalGroup(chatId, ownerId);
        chatAccess.assertOwner(room, ownerId);
        if (newOwnerId == null || newOwnerId.equals(ownerId)) {
            throw new IllegalArgumentException("Выберите нового создателя чата");
        }
        ChatParticipant next = chatAccess.requireParticipant(chatId, newOwnerId);
        next.setAdmin(false);
        chatParticipantRepository.save(next);
        chatParticipantRepository.findByChatIdAndUserId(chatId, ownerId).ifPresent(previous -> {
            previous.setAdmin(true);
            chatParticipantRepository.save(previous);
        });
        room.setOwnerId(newOwnerId);
        chatRoomRepository.save(room);
        UserDto profile = fetchUsers(Set.of(newOwnerId)).get(newOwnerId);
        String name = profile != null ? displayName(profile.getFirstName(), profile.getLastName()) : "Участник";
        postSystem(room, displayName(firstName, lastName) + " передал(а) чат " + name);
        return getMyGroupRooms(ownerId).stream()
                .filter(dto -> dto.getId().equals(chatId))
                .findFirst()
                .orElseThrow();
    }

    @Override
    @Transactional
    public void deletePersonalGroup(Long chatId, Long ownerId) {
        ChatRoom room = requirePersonalGroup(chatId, ownerId);
        chatAccess.assertOwner(room, ownerId);
        deleteRoom(room, "PERSONAL_GROUP_DELETED_BY_OWNER");
    }

    private void removeMemberLocally(ChatRoom room, Long targetUserId, String actorFirstName, String actorLastName) {
        if (chatParticipantRepository.findByChatIdAndUserId(room.getId(), targetUserId).isEmpty()) {
            return;
        }
        UserDto profile = fetchUsers(Set.of(targetUserId)).get(targetUserId);
        String name = profile != null ? displayName(profile.getFirstName(), profile.getLastName()) : "Участник";
        String actor = displayName(actorFirstName, actorLastName);
        if (room.getRoomType() == ChatRoomType.PERSONAL_GROUP) {
            postSystem(room, actor + " исключил(а) " + name + " из чата [KICKED_ID:" + targetUserId + "]");
        } else {
            postSystem(room, name + " исключён(а) из чата [KICKED_ID:" + targetUserId + "]");
        }
        chatParticipantRepository.findByChatIdAndUserId(room.getId(), targetUserId)
                .ifPresent(chatParticipantRepository::delete);
        java.util.Map<String, Object> kicked = new java.util.HashMap<>();
        kicked.put("content", ChatSystemMessages.ROOM_DELETED);
        kicked.put("chatId", room.getId());
        kicked.put("roomDeleted", true);
        kicked.put("isSystem", true);
        messagingTemplate.convertAndSendToUser(String.valueOf(targetUserId), "/queue/messages", kicked);
    }

    private ChatRoom requirePersonalGroup(Long chatId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Чат не найден"));
        if (room.getRoomType() != ChatRoomType.PERSONAL_GROUP) {
            throw new IllegalArgumentException("Это не личный групповой чат");
        }
        requireParticipant(chatId, userId);
        return room;
    }

    private void requireParticipant(Long chatId, Long userId) {
        if (userId == null || chatParticipantRepository.findByChatIdAndUserId(chatId, userId).isEmpty()) {
            throw new SecurityException("Вы не участник этого группового чата");
        }
    }

    private void sendJoinedSignal(ChatRoom room, Long userId) {
        java.util.Map<String, Object> joinedSignal = new java.util.HashMap<>();
        joinedSignal.put("content", "[GROUP_CHAT_JOINED]");
        joinedSignal.put("chatId", room.getId());
        joinedSignal.put("roomType", ChatRoomType.PERSONAL_GROUP.name());
        joinedSignal.put("chatTitle", room.getTitle());
        joinedSignal.put("avatarUrl", room.getAvatarUrl());
        joinedSignal.put("isSystem", true);
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/messages", joinedSignal);
    }

    private static String displayName(String firstName, String lastName) {
        String safeFirst = (firstName == null || firstName.isBlank()) ? "Участник" : firstName.trim();
        String safeLast = lastName == null ? "" : lastName.trim();
        return (safeFirst + " " + safeLast).trim();
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<Long, UserDto> fetchUsers(Set<Long> ids) {
        Map<Long, UserDto> result = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return result;
        }
        try {
            String idsParam = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
            String url = userBaseUrl + "/api/v1/social/users/search/by-ids?ids=" + idsParam;
            ResponseEntity<UserDto[]> response = httpCore.get(new RequestData(url), UserDto[].class);
            if (response != null && response.getBody() != null) {
                for (UserDto user : response.getBody()) {
                    if (user.getUserId() != null) {
                        result.put(user.getUserId(), user);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[Chat] Не удалось загрузить профили участников группы", e);
        }
        return result;
    }
}
