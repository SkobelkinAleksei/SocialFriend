package org.example.chat.service;

import com.example.common.RequestData;
import com.example.common.dto.event.UserDto;
import com.example.common.kafka.NotificationKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chat.entity.ChatFileAttachment;
import org.example.chat.entity.ChatMentions;
import org.example.chat.entity.ChatMessage;
import org.example.chat.entity.ChatMessageDto;
import org.example.chat.entity.ChatPhotoItemDto;
import org.example.chat.entity.ChatPhotoPageDto;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.ChatUserPref;
import org.example.chat.entity.UserChatDto;
import org.example.chat.repository.ChatMessageRepository;
import org.example.chat.repository.ChatParticipantRepository;
import org.example.chat.repository.ChatRoomRepository;
import com.example.common.metrics.AppMetrics;
import org.example.restclient.config.IHttpCore;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageServiceImpl implements ChatMessageService {
    private static final int MAX_CONTENT_LENGTH = 2000;

    private final ChatMessageRepository messageRepository;
    private final IHttpCore httpCore;
    private final NotificationKafkaProducer notificationProducer;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatPreferenceService preferenceService;
    private final ChatPinService pinService;
    private final AppMetrics appMetrics;

    @Value("${app.services.user-base-url:http://localhost:8081}")
    private String userBaseUrl;

    @Override
    @Transactional
    public List<Long> getDistinctChatPartnerIds(Long userId) {
        List<Long> recipients = messageRepository.findRecipientIdsBySenderId(userId);
        List<Long> senders = messageRepository.findSenderIdsByRecipientId(userId);

        Set<Long> partnerIds = new HashSet<>();
        partnerIds.addAll(recipients);
        partnerIds.addAll(senders);
        partnerIds.remove(userId);

        return new ArrayList<>(partnerIds);
    }

    @Override
    @Transactional
    public ChatMessage saveMessage(ChatMessage chatMessage) throws AccessDeniedException {
        log.info("[Chat-Service] Сохранение нового сообщения от пользователя ID: {}", chatMessage.getSenderId());

        validateContentLength(chatMessage.getContent());
        chatMessage.setPhotos(normalizePhotos(chatMessage.getPhotos()));
        chatMessage.setFiles(normalizeFiles(chatMessage.getFiles()));
        chatMessage.setVoiceUrl(normalizeVoiceUrl(chatMessage.getVoiceUrl()));
        chatMessage.setVoiceDuration(normalizeVoiceDuration(chatMessage.getVoiceUrl(), chatMessage.getVoiceDuration()));

        if (chatMessage.getChatId() != null
                && !chatMessage.isSystem()
                && chatMessage.getSenderId() != null
                && chatMessage.getSenderId() > 0) {
            assertGroupParticipant(chatMessage.getChatId(), chatMessage.getSenderId());
        } else if (chatMessage.getRecipientId() != null
                && !chatMessage.isSystem()
                && chatMessage.getSenderId() != null
                && chatMessage.getSenderId() > 0) {
            assertCanMessage(chatMessage.getSenderId(), chatMessage.getRecipientId());
        }

        if (chatMessage.getTimestamp() == null) {
            chatMessage.setTimestamp(LocalDateTime.now());
        }
        final ChatMessage[] dbRootHolder = new ChatMessage[1];

        Long viewerId = chatMessage.getSenderId();
        if (chatMessage.getForwardedFrom() != null && chatMessage.getForwardedFrom().getId() != null) {
            Long targetMessageId = chatMessage.getForwardedFrom().getId();
            ChatMessage clickedMsg = messageRepository.findById(targetMessageId)
                    .orElseThrow(() -> new AccessDeniedException("Нет доступа к сообщению для пересылки"));
            assertCanSeeMessage(clickedMsg, viewerId);

            ChatMessage rootMsg = clickedMsg;
            while (rootMsg.getForwardedFrom() != null) {
                rootMsg = rootMsg.getForwardedFrom();
            }

            messageRepository.findById(rootMsg.getId()).ifPresent(finalRoot -> {
                chatMessage.setForwardedFrom(finalRoot);
                dbRootHolder[0] = finalRoot;
            });

            if (chatMessage.getContent() == null || chatMessage.getContent().trim().isEmpty()) {
                chatMessage.setContent(clickedMsg.getContent());
            }
        } else {
            chatMessage.setForwardedFrom(null);
        }

        if (chatMessage.getParentIds() != null && !chatMessage.getParentIds().isEmpty()) {
            List<ChatMessage> parentMessages = messageRepository.findAllById(chatMessage.getParentIds());
            for (ChatMessage parent : parentMessages) {
                assertCanSeeMessage(parent, viewerId);
            }
            if (!parentMessages.isEmpty()) {
                chatMessage.setReplies(new HashSet<>(parentMessages));
            }
        }
        if (chatMessage.getParentIds() != null && !chatMessage.getParentIds().isEmpty()) {
            List<ChatMessage> forwards = messageRepository.findAllById(chatMessage.getParentIds());
            for (ChatMessage fwd : forwards) {
                assertCanSeeMessage(fwd, viewerId);
            }
            List<ChatMessage> rootForwards = forwards.stream().map(msg -> {
                ChatMessage root = msg;
                while (root.getForwardedFrom() != null) {
                    root = root.getForwardedFrom();
                }
                return root;
            }).toList();

            chatMessage.setBundledForwards(rootForwards);
        } else if (chatMessage.getBundledForwards() != null && !chatMessage.getBundledForwards().isEmpty()) {
            List<Long> bundledIds = chatMessage.getBundledForwards().stream()
                    .map(ChatMessage::getId)
                    .filter(id -> id != null)
                    .toList();
            if (!bundledIds.isEmpty()) {
                List<ChatMessage> loaded = messageRepository.findAllById(bundledIds);
                for (ChatMessage bundled : loaded) {
                    assertCanSeeMessage(bundled, viewerId);
                }
                chatMessage.setBundledForwards(loaded);
            }
        }

        copyForwardedMediaIfMissing(chatMessage);

        ChatMessage savedMessage = messageRepository.save(chatMessage);

        if (!savedMessage.isSystem()) {
            String content = savedMessage.getContent();
            boolean technical = content != null && content.startsWith("[");
            if (!technical) {
                if (savedMessage.getChatId() != null) {
                    appMetrics.chatGruppa();
                    log.info("[Чат] Отправлено групповое сообщение chatId={} senderId={}",
                            savedMessage.getChatId(), savedMessage.getSenderId());
                } else {
                    appMetrics.chatLichnoe();
                    log.info("[Чат] Отправлено личное сообщение recipientId={} senderId={}",
                            savedMessage.getRecipientId(), savedMessage.getSenderId());
                }
            }
        }

        if (savedMessage.getChatId() != null
                && !savedMessage.isSystem()
                && savedMessage.getSenderId() != null
                && savedMessage.getSenderId() > 0) {
            chatParticipantRepository.incrementUnreadExcept(savedMessage.getChatId(), savedMessage.getSenderId());
        }

        if (savedMessage.getForwardedFrom() != null && dbRootHolder[0] != null) {
            Hibernate.initialize(dbRootHolder[0]);
            savedMessage.setForwardedFrom(dbRootHolder[0]);
        }

        if (savedMessage.getReplies() != null && !savedMessage.getReplies().isEmpty()) {
            Hibernate.initialize(savedMessage.getReplies());
            savedMessage.getReplies().forEach(reply -> {
                if (reply.getSenderFirstName() != null) reply.getSenderFirstName().trim();
                if (reply.getContent() != null) reply.getContent().trim();
            });
        }

        if (chatMessage.getParentIds() != null) {
            savedMessage.setParentIds(chatMessage.getParentIds());
        }

        if (savedMessage.getForwardedFrom() != null) {
            if (savedMessage.getForwardedFrom().getSenderFirstName() != null) {
                savedMessage.getForwardedFrom().getSenderFirstName().trim();
            }
            if (savedMessage.getForwardedFrom().getSenderLastName() != null) {
                savedMessage.getForwardedFrom().getSenderLastName().trim();
            }
        }

        if (savedMessage.getChatId() == null) {
            if (!preferenceService.isMuted(savedMessage.getRecipientId(), ChatAccess.personalScope(savedMessage.getSenderId()))) {
                boolean mentioned = ChatMentions.parse(savedMessage.getContent()).contains(savedMessage.getRecipientId());
                notificationProducer.sendEvent(
                        savedMessage.getRecipientId(),
                        savedMessage.getSenderId(),
                        savedMessage.getSenderFirstName(),
                        savedMessage.getSenderLastName(),
                        com.example.common.kafka.NotificationType.NEW_CHAT_MESSAGE,
                        savedMessage.getId(),
                        null,
                        previewContent(savedMessage),
                        mentioned ? "MENTION:PERSONAL" : "PERSONAL"
                );
            }
        } else {
            String roomTitle = chatRoomRepository.findById(savedMessage.getChatId())
                    .map(ChatRoom::getTitle)
                    .orElse("Встреча");
            String contextLabel = "GROUP:" + roomTitle;
            java.util.List<Long> mentioned = ChatMentions.parse(savedMessage.getContent());

            List<Long> participantIds = chatParticipantRepository.findUserIdsByChatId(savedMessage.getChatId());

            if (participantIds != null) {
                for (Long participantId : participantIds) {
                    if (!participantId.equals(savedMessage.getSenderId())) {
                        if (preferenceService.isMuted(participantId, ChatAccess.roomScope(savedMessage.getChatId()))) {
                            continue;
                        }
                        boolean isMention = mentioned.contains(participantId);
                        notificationProducer.sendEvent(
                                participantId,
                                savedMessage.getSenderId(),
                                savedMessage.getSenderFirstName(),
                                savedMessage.getSenderLastName(),
                                com.example.common.kafka.NotificationType.NEW_CHAT_MESSAGE,
                                savedMessage.getChatId(),
                                null,
                                previewContent(savedMessage),
                                isMention ? "MENTION:" + contextLabel : contextLabel
                        );
                    }
                }
            }
        }

        return savedMessage;
    }

    @Override
    @Transactional
    public List<ChatMessageDto> getChatHistory(Long senderId, Long recipientId) {
        List<ChatMessage> history = messageRepository.findChatHistory(senderId, recipientId);

        if (history != null && !history.isEmpty()) {
            ChatMessage ultimateMsg = history.get(history.size() - 1);
            if (isDeleteMarkerFor(ultimateMsg.getContent(), senderId)) {
                messageRepository.delete(ultimateMsg);
                history.remove(history.size() - 1);
            }
        }

        if (history == null || history.isEmpty()) {
            return List.of();
        }

        int cutIndex = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            if (isClearMarkerFor(history.get(i).getContent(), senderId)) {
                cutIndex = i + 1;
                break;
            }
        }

        List<ChatMessage> visibleHistory = history.subList(cutIndex, history.size());

        List<Long> ids = visibleHistory.stream()
                .filter(msg -> !isTechnicalPersonalMarker(msg.getContent()))
                .map(ChatMessage::getId)
                .toList();
        return convertToDtoList(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getChatHistoryPage(Long senderId, Long recipientId, Long beforeId, int size) {
        int pageSize = Math.min(Math.max(size, 1), 100);
        Long minId = messageRepository.findPersonalClearCutId(senderId, recipientId, senderId);
        if (minId == null) minId = 0L;
        Long before = beforeId == null ? Long.MAX_VALUE : beforeId;
        List<Long> idsDesc = messageRepository.findPersonalHistoryPageIds(
                senderId, recipientId, minId, before,
                org.springframework.data.domain.PageRequest.of(0, pageSize));
        if (idsDesc == null || idsDesc.isEmpty()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>(idsDesc);
        java.util.Collections.reverse(ids);
        return convertToDtoList(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long recipientId) {
        return messageRepository.countUnreadMessages(recipientId);
    }

    @Override
    @Transactional
    public void readAllMessagesFromUser(Long senderId, Long recipientId) {
        messageRepository.markMessagesAsRead(senderId, recipientId);
    }

    @Override
    @Transactional
    public ChatMessage editMessage(Long messageId, String newContent, Long senderId) {
        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Сообщение не найдено"));

        if (!message.getSenderId().equals(senderId)) {
            throw new SecurityException("Вы можете редактировать только свои сообщения");
        }

        validateContentLength(newContent);
        message.setContent(newContent);
        message.setEdited(true);
        return messageRepository.save(message);
    }

    @Override
    @Transactional
    public void deleteMessage(Long messageId, Long senderId) {
        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Сообщение не найдено"));

        if (!message.getSenderId().equals(senderId)) {
            throw new SecurityException("Вы можете удалять только свои сообщения");
        }

        message.setDeleted(true);
        messageRepository.save(message);
        pinService.removePinForDeletedMessage(messageId);
    }

    @Override
    @Transactional
    public void deleteMessagesBatch(List<Long> messageIds, Long senderId) {
        if (messageIds == null || messageIds.isEmpty()) return;

        for (Long id : messageIds) {
            ChatMessage message = messageRepository.findById(id).orElse(null);
            if (message != null && message.getSenderId().equals(senderId)) {
                message.setDeleted(true);
                messageRepository.save(message);
                pinService.removePinForDeletedMessage(id);
            }
        }
    }

    @Override
    @Transactional
    public void clearChat(Long user1, Long user2) {
        log.info("[Chat-Service] Очистка истории только для пользователя {} (партнёр {})", user1, user2);

        ChatMessage clearMarker = ChatMessage.builder()
                .senderId(user1)
                .recipientId(user2)
                .content("[CHAT_HISTORY_CLEARED:" + user1 + "]")
                .timestamp(LocalDateTime.now())
                .isSystem(true)
                .read(true)
                .build();
        messageRepository.save(clearMarker);
    }

    @Override
    @Transactional
    public void deletePersonalChatForUser(Long userId, Long partnerId) {
        log.info("[Chat-Service] Скрытие чата из списка только для пользователя {} (партнёр {})", userId, partnerId);

        ChatMessage deletedMarker = ChatMessage.builder()
                .senderId(userId)
                .recipientId(partnerId)
                .content("[CHAT_DELETED_BY_USER:" + userId + "]")
                .timestamp(LocalDateTime.now())
                .isSystem(true)
                .read(true)
                .build();
        messageRepository.save(deletedMarker);
    }

    @Override
    @Transactional
    public ChatMessage findById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Сообщение не найдено"));
    }

    @Override
    @Transactional
    public void initializePersonalChat(Long senderId, Long recipientId) throws AccessDeniedException {
        log.info("[Chat-Service] Инициализация личного чата между {} и {}", senderId, recipientId);

        List<ChatMessage> history = messageRepository.findChatHistory(senderId, recipientId);

        if (history != null && !history.isEmpty()) {
            ChatMessage ultimateMsg = history.get(history.size() - 1);
            if (isDeleteMarkerFor(ultimateMsg.getContent(), senderId)) {
                messageRepository.delete(ultimateMsg);
            }
            return;
        }

        log.info("[Chat-Service] Переписка пуста. Запрашиваем данные собеседника через user-base-url...");

        UserDto userDto = fetchUserProfile(recipientId, senderId);
        if (userDto == null) {
            throw new AccessDeniedException("Не удалось проверить права на сообщения. Попробуйте позже.");
        }
        if (!userDto.isCanMessage()) {
            log.warn("[Chat-Service] Блокировка создания чата: пользователь {} запретил DM от {}", recipientId, senderId);
            throw new AccessDeniedException(
                    "Пользователь ограничил круг лиц, которые могут писать ему сообщения."
            );
        }

        ChatMessage hiddenMarker = ChatMessage.builder()
                .senderId(senderId)
                .recipientId(recipientId)
                .senderFirstName(userDto.getFirstName())
                .senderLastName(userDto.getLastName())
                .content("[CHAT_CREATED]")
                .timestamp(LocalDateTime.now())
                .isSystem(true)
                .read(true)
                .build();

        messageRepository.save(hiddenMarker);
        log.info("[Chat-Service] Стартовый маркер успешно записан в PostgreSQL.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserChatDto> getUserChats(Long userId) {
        List<Long> recipients = messageRepository.findRecipientIdsBySenderId(userId);
        List<Long> senders = messageRepository.findSenderIdsByRecipientId(userId);
        Set<Long> partnerIds = new HashSet<>();
        if (recipients != null) partnerIds.addAll(recipients);
        if (senders != null) partnerIds.addAll(senders);
        partnerIds.remove(userId);

        List<UserChatDto> dtoList = new ArrayList<>();

        for (Long partnerId : partnerIds) {
            List<ChatMessage> history = messageRepository.findChatHistory(userId, partnerId);
            if (history == null || history.isEmpty()) continue;

            ChatMessage ultimateMsg = history.get(history.size() - 1);
            if (isDeleteMarkerFor(ultimateMsg.getContent(), userId)) {
                continue;
            }

            int cutIndex = 0;
            for (int i = history.size() - 1; i >= 0; i--) {
                if (isClearMarkerFor(history.get(i).getContent(), userId)) {
                    cutIndex = i + 1;
                    break;
                }
            }

            List<ChatMessage> visibleHistory = history.subList(cutIndex, history.size());

            ChatMessage lastMsg = null;
            for (int i = visibleHistory.size() - 1; i >= 0; i--) {
                ChatMessage msg = visibleHistory.get(i);
                if (!msg.isSystem()) {
                    lastMsg = msg;
                    break;
                }
            }

            boolean isChatEmptyAfterClear = (lastMsg == null);

            String contentText;
            if (isChatEmptyAfterClear) {
                contentText = "Переписка создана";
            } else if (lastMsg.isDeleted()) {
                contentText = "Сообщение удалено";
            } else {
                contentText = "[CHAT_CREATED]".equals(lastMsg.getContent())
                        ? "Переписка создана"
                        : sidebarPreview(lastMsg);
            }

            String partnerFirstName = "";
            String partnerLastName = "";
            String partnerAvatar = "";
            boolean partnerOnline = false;
            java.time.LocalDateTime partnerLastSeen = null;
            try {
                String userUrl = userBaseUrl + "/api/v1/social/users/" + partnerId;
                RequestData requestData = new RequestData(userUrl);
                ResponseEntity<UserDto> response = httpCore.get(requestData, UserDto.class);
                if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    UserDto userDto = response.getBody();
                    partnerFirstName = userDto.getFirstName();
                    partnerLastName = userDto.getLastName();
                    partnerAvatar = userDto.getAvatarUrl();
                    partnerOnline = userDto.isOnline();
                    partnerLastSeen = userDto.getLastSeenAt();
                }
            } catch (Exception e) {
                log.error("[httpCore] Ошибка получения имени собеседника {}", partnerId, e);
            }

            if (partnerFirstName == null || partnerFirstName.isEmpty()) {
                if ("[CHAT_CREATED]".equals(history.get(0).getContent()) && history.get(0).getSenderId().equals(userId)) {
                    partnerFirstName = history.get(0).getSenderFirstName();
                    partnerLastName = history.get(0).getSenderLastName();
                }
            }

            String fullName = ((partnerFirstName != null ? partnerFirstName : "") + " " + (partnerLastName != null ? partnerLastName : "")).trim();
            if (fullName.isEmpty()) {
                fullName = "Пользователь " + partnerId;
            }

            long unreadCount = history.stream().filter(m -> m.getRecipientId().equals(userId) && !m.isRead()).count();

            java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            String timeStr = "";
            LocalDateTime actualTimestampForSort = null;

            if (lastMsg != null) {
                timeStr = lastMsg.getTimestamp() != null ? lastMsg.getTimestamp().format(timeFormatter) : "";
                actualTimestampForSort = lastMsg.getTimestamp();
            } else {
                timeStr = ultimateMsg.getTimestamp() != null ? ultimateMsg.getTimestamp().format(timeFormatter) : "";
                actualTimestampForSort = ultimateMsg.getTimestamp();
            }

            UserChatDto dto = UserChatDto.builder()
                    .id(partnerId.toString())
                    .name(fullName)
                    .avatar(partnerAvatar != null ? partnerAvatar : "")
                    .last_message(contentText)
                    .time(timeStr)
                    .unread((int) unreadCount)
                    .online(partnerOnline)
                    .lastSeenAt(partnerLastSeen)
                    .isDeleted(isChatEmptyAfterClear)
                    .lastMessageId(lastMsg != null ? lastMsg.getId() : ultimateMsg.getId())
                    .build();

            dto.setActualTimestamp(actualTimestampForSort);
            dtoList.add(dto);
        }
        java.util.List<String> scopeKeys = dtoList.stream()
                .map(dto -> ChatAccess.personalScope(Long.valueOf(dto.getId())))
                .toList();
        java.util.Map<String, ChatUserPref> prefs = preferenceService.mapForUser(userId, scopeKeys);
        for (UserChatDto dto : dtoList) {
            ChatUserPref pref = prefs.get(ChatAccess.personalScope(Long.valueOf(dto.getId())));
            if (pref != null) {
                dto.setMuted(pref.isMuted());
                dto.setPinned(pref.getPinnedAt() != null);
                dto.setPinnedAt(pref.getPinnedAt());
            }
        }

        dtoList.sort((a, b) -> {
            LocalDateTime timeA = a.getActualTimestamp() != null ? a.getActualTimestamp() : LocalDateTime.MIN;
            LocalDateTime timeB = b.getActualTimestamp() != null ? b.getActualTimestamp() : LocalDateTime.MIN;
            return timeB.compareTo(timeA);
        });

        return dtoList;
    }

    @Override
    @Transactional(readOnly = true)
    public ChatMessageDto convertToDto(Long messageId) {
        ChatMessageDto dto = toDtoCore(messageId);
        attachSenderAvatars(List.of(dto));
        dto.setPinned(pinService.pinnedIds(List.of(messageId)).contains(messageId));
        return dto;
    }

    private ChatMessageDto toDtoCore(Long messageId) {
        ChatMessage msg = messageRepository.findByIdWithForwardedFrom(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Сообщение не найдено"));

        ChatMessageDto.ForwardedFromDto forwardedDto = null;
        if (msg.getForwardedFrom() != null) {
            ChatMessage original = msg.getForwardedFrom();
            if (original.getPhotos() != null) {
                Hibernate.initialize(original.getPhotos());
            }
            if (original.getFiles() != null) {
                Hibernate.initialize(original.getFiles());
            }
            forwardedDto = ChatMessageDto.ForwardedFromDto.builder()
                    .id(original.getId())
                    .senderId(original.getSenderId())
                    .senderFirstName(original.getSenderFirstName())
                    .senderLastName(original.getSenderLastName())
                    .content(original.getContent())
                    .photos(copyPhotos(original.getPhotos()))
                    .files(copyFiles(original.getFiles()))
                    .voiceUrl(original.getVoiceUrl())
                    .voiceDuration(original.getVoiceDuration())
                    .build();
        }

        List<ChatMessageDto.ForwardedFromDto> bundledList = new ArrayList<>();
        if (msg.getBundledForwards() != null && !msg.getBundledForwards().isEmpty()) {
            for (ChatMessage fb : msg.getBundledForwards()) {
                boolean isFwDeleted = fb.isDeleted();
                String finalContent = isFwDeleted ? "Сообщение удалено" : fb.getContent();
                if (fb.getPhotos() != null) {
                    Hibernate.initialize(fb.getPhotos());
                }
                if (fb.getFiles() != null) {
                    Hibernate.initialize(fb.getFiles());
                }

                bundledList.add(ChatMessageDto.ForwardedFromDto.builder()
                        .id(fb.getId())
                        .senderId(fb.getSenderId())
                        .senderFirstName(fb.getSenderFirstName())
                        .senderLastName(fb.getSenderLastName())
                        .content(finalContent)
                        .photos(isFwDeleted ? new ArrayList<>() : copyPhotos(fb.getPhotos()))
                        .files(isFwDeleted ? new ArrayList<>() : copyFiles(fb.getFiles()))
                        .voiceUrl(isFwDeleted ? null : fb.getVoiceUrl())
                        .voiceDuration(isFwDeleted ? null : fb.getVoiceDuration())
                        .build());
            }
        }

        Set<Long> repliesIds = new HashSet<>();
        if (msg.getReplies() != null && !msg.getReplies().isEmpty()) {
            Hibernate.initialize(msg.getReplies());
            for (ChatMessage reply : msg.getReplies()) {
                if (reply.getId() != null) {
                    repliesIds.add(reply.getId());
                }
            }
        } else if (msg.getParentIds() != null) {
            repliesIds.addAll(msg.getParentIds());
        }

        if (msg.getPhotos() != null) {
            Hibernate.initialize(msg.getPhotos());
        }
        if (msg.getFiles() != null) {
            Hibernate.initialize(msg.getFiles());
        }

        return ChatMessageDto.builder()
                .id(msg.getId())
                .senderId(msg.getSenderId())
                .recipientId(msg.getRecipientId())
                .senderFirstName(msg.getSenderFirstName())
                .senderLastName(msg.getSenderLastName())
                .chatId(msg.getChatId())
                .content(msg.getContent())
                .photos(copyPhotos(msg.getPhotos()))
                .files(copyFiles(msg.getFiles()))
                .voiceUrl(msg.getVoiceUrl())
                .voiceDuration(msg.getVoiceDuration())
                .timestamp(msg.getTimestamp())
                .read(msg.isRead())
                .isSystem(msg.isSystem())
                .edited(msg.isEdited())
                .deleted(msg.isDeleted())
                .parentIds(repliesIds)
                .forwardedFrom(forwardedDto)
                .bundledForwards(bundledList)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> convertToDtoList(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return List.of();
        }
        List<ChatMessageDto> dtos = new ArrayList<>();
        for (Long id : messageIds) {
            dtos.add(toDtoCore(id));
        }
        attachSenderAvatars(dtos);
        java.util.Set<Long> pinned = pinService.pinnedIds(dtos.stream()
                .map(ChatMessageDto::getId)
                .filter(java.util.Objects::nonNull)
                .toList());
        for (ChatMessageDto dto : dtos) {
            dto.setPinned(dto.getId() != null && pinned.contains(dto.getId()));
        }
        return dtos;
    }

    private void attachSenderAvatars(List<ChatMessageDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;
        Set<Long> senderIds = dtos.stream()
                .map(ChatMessageDto::getSenderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> avatars = fetchAvatarUrls(senderIds);
        for (ChatMessageDto dto : dtos) {
            if (dto.getSenderId() != null) {
                dto.setSenderAvatarUrl(avatars.get(dto.getSenderId()));
            }
        }
    }

    private Map<Long, String> fetchAvatarUrls(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        try {
            String idsParam = userIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            String url = userBaseUrl + "/api/v1/social/users/search/by-ids?ids=" + idsParam;
            ResponseEntity<UserDto[]> response = httpCore.get(new RequestData(url), UserDto[].class);
            if (response != null && response.getBody() != null) {
                Map<Long, String> result = new HashMap<>();
                for (UserDto user : response.getBody()) {
                    if (user.getUserId() != null) {
                        result.put(user.getUserId(), user.getAvatarUrl());
                    }
                }
                return result;
            }
        } catch (Exception e) {
            log.error("[Chat-Service] Не удалось пакетно загрузить аватарки отправителей", e);
        }
        return Map.of();
    }

    private void validateContentLength(String content) {
        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException(
                    "Сообщение слишком длинное (макс. " + MAX_CONTENT_LENGTH + " символов)");
        }
    }

    private void copyForwardedMediaIfMissing(ChatMessage chatMessage) {
        boolean hasPhotos = chatMessage.getPhotos() != null && !chatMessage.getPhotos().isEmpty();
        boolean hasFiles = chatMessage.getFiles() != null && !chatMessage.getFiles().isEmpty();
        boolean hasVoice = chatMessage.getVoiceUrl() != null && !chatMessage.getVoiceUrl().isBlank();
        if (hasPhotos || hasFiles || hasVoice) {
            return;
        }
        ChatMessage source = null;
        if (chatMessage.getForwardedFrom() != null) {
            source = chatMessage.getForwardedFrom();
        } else if (chatMessage.getBundledForwards() != null && !chatMessage.getBundledForwards().isEmpty()) {
            source = chatMessage.getBundledForwards().get(0);
        }
        source = findMediaSource(source, new HashSet<>());
        if (source == null) {
            return;
        }
        if (source.getPhotos() != null && !source.getPhotos().isEmpty()) {
            chatMessage.setPhotos(copyPhotos(source.getPhotos()));
        }
        if (source.getFiles() != null && !source.getFiles().isEmpty()) {
            chatMessage.setFiles(copyFiles(source.getFiles()));
        }
        if (source.getVoiceUrl() != null && !source.getVoiceUrl().isBlank()) {
            chatMessage.setVoiceUrl(source.getVoiceUrl());
            chatMessage.setVoiceDuration(source.getVoiceDuration());
        }
    }

    private ChatMessage findMediaSource(ChatMessage source, Set<Long> visited) {
        if (source == null) {
            return null;
        }
        if (source.getId() != null && !visited.add(source.getId())) {
            return null;
        }
        try {
            if (source.getPhotos() != null) Hibernate.initialize(source.getPhotos());
            if (source.getFiles() != null) Hibernate.initialize(source.getFiles());
            if (source.getBundledForwards() != null) Hibernate.initialize(source.getBundledForwards());
        } catch (Exception ignored) {
        }
        boolean hasPhotos = source.getPhotos() != null && !source.getPhotos().isEmpty();
        boolean hasFiles = source.getFiles() != null && !source.getFiles().isEmpty();
        boolean hasVoice = source.getVoiceUrl() != null && !source.getVoiceUrl().isBlank();
        if (hasPhotos || hasFiles || hasVoice) {
            return source;
        }
        if (source.getForwardedFrom() != null) {
            ChatMessage nested = findMediaSource(source.getForwardedFrom(), visited);
            if (nested != null) {
                return nested;
            }
        }
        if (source.getBundledForwards() != null) {
            for (ChatMessage bundled : source.getBundledForwards()) {
                ChatMessage nested = findMediaSource(bundled, visited);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static final int MAX_PHOTOS = 10;
    private static final int MAX_FILES = 5;
    private static final int MAX_VOICE_DURATION = 300;
    private static final String CHAT_MEDIA_PREFIX = "/api/v1/social/chats/media/";
    private static final String POST_MEDIA_PREFIX = "/api/v1/social/posts/media/";
    private static final String EVENT_MEDIA_PREFIX = "/api/v1/social/events/media/";
    private static final String USER_MEDIA_PREFIX = "/api/v1/social/users/media/";
    private static final String STORED_FILE_PATTERN = "^[a-fA-F0-9\\-]{36}\\.[a-zA-Z0-9]{2,8}$";
    private static final String PHOTO_FILE_PATTERN = "^[a-fA-F0-9\\-]{36}\\.(jpg|jpeg|png|webp|gif)$";
    private static final String VOICE_FILE_PATTERN = "^[a-fA-F0-9\\-]{36}\\.(webm|ogg|mp3|m4a|wav|aac)$";

    private static boolean isAllowedPhotoUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String[] prefixes = {CHAT_MEDIA_PREFIX, POST_MEDIA_PREFIX, EVENT_MEDIA_PREFIX, USER_MEDIA_PREFIX};
        for (String prefix : prefixes) {
            if (url.startsWith(prefix) && url.substring(prefix.length()).matches(PHOTO_FILE_PATTERN)) {
                return true;
            }
        }
        return false;
    }

    private List<String> normalizePhotos(List<String> photos) {
        if (photos == null || photos.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> cleaned = photos.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(String::trim)
                .filter(ChatMessageServiceImpl::isAllowedPhotoUrl)
                .distinct()
                .limit(MAX_PHOTOS)
                .collect(java.util.stream.Collectors.toList());
        if (photos.stream().filter(url -> url != null && !url.isBlank()).count() > MAX_PHOTOS) {
            throw new IllegalArgumentException("Можно прикрепить не больше 10 фотографий.");
        }
        return cleaned;
    }

    private List<ChatFileAttachment> normalizeFiles(List<ChatFileAttachment> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }
        long incoming = files.stream().filter(f -> f != null && f.getUrl() != null && !f.getUrl().isBlank()).count();
        if (incoming > MAX_FILES) {
            throw new IllegalArgumentException("Можно прикрепить не больше 5 файлов.");
        }
        List<ChatFileAttachment> cleaned = new ArrayList<>();
        for (ChatFileAttachment file : files) {
            if (file == null || file.getUrl() == null || file.getUrl().isBlank()) continue;
            String url = file.getUrl().trim();
            if (!url.startsWith(CHAT_MEDIA_PREFIX)
                    || !url.substring(CHAT_MEDIA_PREFIX.length()).matches(STORED_FILE_PATTERN)) {
                continue;
            }
            String name = file.getName() == null ? "file" : file.getName().replaceAll("[\\r\\n\\t]", " ").trim();
            if (name.length() > 180) name = name.substring(0, 180);
            if (name.isBlank()) name = "file";
            Long size = file.getSize() == null || file.getSize() < 0 ? 0L : Math.min(file.getSize(), 20L * 1024 * 1024);
            cleaned.add(ChatFileAttachment.builder()
                    .url(url)
                    .name(name)
                    .size(size)
                    .mimeType(file.getMimeType())
                    .build());
            if (cleaned.size() >= MAX_FILES) break;
        }
        return cleaned;
    }

    private String normalizeVoiceUrl(String voiceUrl) {
        if (voiceUrl == null || voiceUrl.isBlank()) return null;
        String url = voiceUrl.trim();
        if (!url.startsWith(CHAT_MEDIA_PREFIX)
                || !url.substring(CHAT_MEDIA_PREFIX.length()).matches(VOICE_FILE_PATTERN)) {
            return null;
        }
        return url;
    }

    private Integer normalizeVoiceDuration(String voiceUrl, Integer duration) {
        if (voiceUrl == null || voiceUrl.isBlank()) return null;
        int value = duration == null ? 1 : duration;
        return Math.max(1, Math.min(value, MAX_VOICE_DURATION));
    }

    private static List<String> copyPhotos(List<String> photos) {
        if (photos == null || photos.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(photos);
    }

    private static List<ChatFileAttachment> copyFiles(List<ChatFileAttachment> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }
        List<ChatFileAttachment> copy = new ArrayList<>();
        for (ChatFileAttachment file : files) {
            if (file == null) continue;
            copy.add(ChatFileAttachment.builder()
                    .url(file.getUrl())
                    .name(file.getName())
                    .size(file.getSize())
                    .mimeType(file.getMimeType())
                    .build());
        }
        return copy;
    }

    public static List<ChatPhotoItemDto> collectPhotoItems(List<ChatMessageDto> messages) {
        List<ChatPhotoItemDto> items = new ArrayList<>();
        if (messages == null) {
            return items;
        }
        for (ChatMessageDto msg : messages) {
            if (msg == null || msg.isDeleted() || msg.getPhotos() == null) {
                continue;
            }
            for (String url : msg.getPhotos()) {
                if (url != null && !url.isBlank()) {
                    items.add(withSender(new ChatPhotoItemDto(msg.getId(), url, msg.getTimestamp()), msg));
                }
            }
        }
        sortMediaItems(items);
        return items;
    }

    public static List<ChatPhotoItemDto> collectFileItems(List<ChatMessageDto> messages) {
        List<ChatPhotoItemDto> items = new ArrayList<>();
        if (messages == null) {
            return items;
        }
        for (ChatMessageDto msg : messages) {
            if (msg == null || msg.isDeleted() || msg.getFiles() == null) {
                continue;
            }
            for (ChatFileAttachment file : msg.getFiles()) {
                if (file == null || file.getUrl() == null || file.getUrl().isBlank()) continue;
                ChatPhotoItemDto item = withSender(new ChatPhotoItemDto(msg.getId(), file.getUrl(), msg.getTimestamp()), msg);
                item.setName(file.getName());
                item.setSize(file.getSize());
                item.setMimeType(file.getMimeType());
                items.add(item);
            }
        }
        sortMediaItems(items);
        return items;
    }

    public static List<ChatPhotoItemDto> collectVoiceItems(List<ChatMessageDto> messages) {
        List<ChatPhotoItemDto> items = new ArrayList<>();
        if (messages == null) {
            return items;
        }
        for (ChatMessageDto msg : messages) {
            if (msg == null || msg.isDeleted() || msg.getVoiceUrl() == null || msg.getVoiceUrl().isBlank()) {
                continue;
            }
            ChatPhotoItemDto item = withSender(new ChatPhotoItemDto(msg.getId(), msg.getVoiceUrl(), msg.getTimestamp()), msg);
            item.setDuration(msg.getVoiceDuration());
            item.setName("Голосовое сообщение");
            items.add(item);
        }
        sortMediaItems(items);
        return items;
    }

    private static ChatPhotoItemDto withSender(ChatPhotoItemDto item, ChatMessageDto msg) {
        if (item == null || msg == null) {
            return item;
        }
        item.setSenderId(msg.getSenderId());
        item.setSenderFirstName(msg.getSenderFirstName());
        item.setSenderLastName(msg.getSenderLastName());
        return item;
    }

    private static void sortMediaItems(List<ChatPhotoItemDto> items) {
        items.sort((a, b) -> {
            if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });
    }

    public static ChatPhotoPageDto paginatePhotoItems(List<ChatPhotoItemDto> all, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? 24 : Math.min(size, 60);
        long total = all == null ? 0 : all.size();
        int from = (int) Math.min((long) safePage * safeSize, total);
        int to = (int) Math.min((long) from + safeSize, total);
        List<ChatPhotoItemDto> slice = (all == null || from >= to)
                ? new ArrayList<>()
                : new ArrayList<>(all.subList(from, to));
        return new ChatPhotoPageDto(slice, safePage, safeSize, total, to < total);
    }

    public static String sidebarPreview(ChatMessage msg) {
        if (msg == null) {
            return "Переписка создана";
        }
        String content = msg.getContent();
        int photoCount = 0;
        int fileCount = 0;
        String firstFileName = null;
        boolean hasVoice = false;
        try {
            if (msg.getPhotos() != null) {
                Hibernate.initialize(msg.getPhotos());
                photoCount = (int) msg.getPhotos().stream()
                        .filter(url -> url != null && !url.isBlank())
                        .count();
            }
            if (msg.getFiles() != null) {
                Hibernate.initialize(msg.getFiles());
                fileCount = (int) msg.getFiles().stream()
                        .filter(f -> f != null && f.getUrl() != null && !f.getUrl().isBlank())
                        .count();
                firstFileName = msg.getFiles().stream()
                        .filter(f -> f != null && f.getName() != null && !f.getName().isBlank())
                        .map(ChatFileAttachment::getName)
                        .findFirst()
                        .orElse(null);
            }
            hasVoice = msg.getVoiceUrl() != null && !msg.getVoiceUrl().isBlank();
        } catch (Exception ignored) {
            // lazy collection вне сессии
        }
        boolean hasText = content != null && !content.isBlank();
        if (hasText) {
            if (content.contains("[SHARE_POST:")) {
                int nl = content.indexOf('\n');
                if (nl >= 0 && nl < content.length() - 1) {
                    String comment = content.substring(nl + 1).trim();
                    if (!comment.isBlank()) {
                        return comment;
                    }
                }
                return "Поделился постом";
            }
            if (content.contains("[SHARE_EVENT:")) {
                int nl = content.indexOf('\n');
                if (nl >= 0 && nl < content.length() - 1) {
                    String comment = content.substring(nl + 1).trim();
                    if (!comment.isBlank()) {
                        return comment;
                    }
                }
                return "Поделился встречей";
            }
            return content.replaceAll("\\[MENTION:\\d+\\]", "").trim();
        }
        if (hasVoice) {
            return "Голосовое сообщение";
        }
        if (fileCount > 0) {
            if (fileCount > 1) return "Файл (" + fileCount + ")";
            return firstFileName != null ? firstFileName : "Файл";
        }
        if (photoCount > 0) {
            return photoCount > 1 ? "Фото (" + photoCount + ")" : "Фото";
        }
        return "Сообщение";
    }

    @Override
    @Transactional(readOnly = true)
    public ChatPhotoPageDto getPersonalChatPhotos(Long senderId, Long recipientId, int page, int size) {
        return paginatePhotoItems(collectPhotoItems(getChatHistory(senderId, recipientId)), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatPhotoPageDto getPersonalChatFiles(Long senderId, Long recipientId, int page, int size) {
        return paginatePhotoItems(collectFileItems(getChatHistory(senderId, recipientId)), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatPhotoPageDto getPersonalChatVoices(Long senderId, Long recipientId, int page, int size) {
        return paginatePhotoItems(collectVoiceItems(getChatHistory(senderId, recipientId)), page, size);
    }

    private static String previewContent(ChatMessage message) {
        return sidebarPreview(message);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCanSignal(Long userId, Long chatId, Long recipientId) throws AccessDeniedException {
        if (chatId != null) {
            assertGroupParticipant(chatId, userId);
            return;
        }
        if (recipientId == null || recipientId.equals(userId)) {
            throw new AccessDeniedException("Некорректный сигнал набора");
        }
        assertCanMessage(userId, recipientId);
    }

    private void assertGroupParticipant(Long chatId, Long userId) throws AccessDeniedException {
        if (userId == null || chatParticipantRepository.findByChatIdAndUserId(chatId, userId).isEmpty()) {
            throw new AccessDeniedException("Вы не участник этого группового чата");
        }
    }

    private void assertCanSeeMessage(ChatMessage message, Long userId) throws AccessDeniedException {
        if (message == null || userId == null) {
            throw new AccessDeniedException("Нет доступа к сообщению");
        }
        if (message.getChatId() != null) {
            assertGroupParticipant(message.getChatId(), userId);
            return;
        }
        if (userId.equals(message.getSenderId()) || userId.equals(message.getRecipientId())) {
            return;
        }
        throw new AccessDeniedException("Нет доступа к сообщению");
    }

    private void assertCanMessage(Long senderId, Long recipientId) throws AccessDeniedException {
        UserDto userDto = fetchUserProfile(recipientId, senderId);
        if (userDto == null) {
            throw new AccessDeniedException("Не удалось проверить права на сообщения. Попробуйте позже.");
        }
        if (!userDto.isCanMessage()) {
            throw new AccessDeniedException(
                    "Пользователь ограничил круг лиц, которые могут писать ему сообщения."
            );
        }
    }

    private UserDto fetchUserProfile(Long targetUserId, Long viewerId) {
        try {
            String userUrl = userBaseUrl + "/api/v1/social/users/" + targetUserId + "/profile";
            RequestData requestData = new RequestData(
                    userUrl,
                    Map.of("X-User-Id", String.valueOf(viewerId))
            );
            ResponseEntity<UserDto> response = httpCore.get(requestData, UserDto.class);
            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("[httpCore] Не удалось получить профиль пользователя {}", targetUserId, e);
        }
        return null;
    }

    private static boolean isClearMarkerFor(String content, Long userId) {
        if (content == null) return false;
        if ("[CHAT_HISTORY_CLEARED]".equals(content)) return true;
        return content.equals("[CHAT_HISTORY_CLEARED:" + userId + "]");
    }

    private static boolean isDeleteMarkerFor(String content, Long userId) {
        if (content == null) return false;
        if ("[CHAT_DELETED_BY_USER]".equals(content)) return true;
        return content.equals("[CHAT_DELETED_BY_USER:" + userId + "]");
    }

    private static boolean isTechnicalPersonalMarker(String content) {
        if (content == null) return false;
        return content.equals("[CHAT_HISTORY_CLEARED]")
                || content.equals("[CHAT_DELETED_BY_USER]")
                || content.startsWith("[CHAT_HISTORY_CLEARED:")
                || content.startsWith("[CHAT_DELETED_BY_USER:");
    }
}
