package org.example.chat.service;

import org.example.chat.entity.ChatMessage;
import org.example.chat.entity.ChatMessageDto;
import org.example.chat.entity.ChatPhotoPageDto;
import org.example.chat.entity.UserChatDto;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.List;

public interface ChatMessageService {
    List<Long> getDistinctChatPartnerIds(Long userId);
    List<UserChatDto> getUserChats(Long userId);
    ChatMessage saveMessage(ChatMessage chatMessage) throws AccessDeniedException;
    List<ChatMessageDto> getChatHistory(Long senderId, Long recipientId);
    List<ChatMessageDto> getChatHistoryPage(Long senderId, Long recipientId, Long beforeId, int size);
    long getUnreadCount(Long recipientId);
    void readAllMessagesFromUser(Long senderId, Long recipientId);
    ChatMessage editMessage(Long messageId, String newContent, Long senderId);
    void deleteMessage(Long messageId, Long senderId);
    void deleteMessagesBatch(List<Long> messageIds, Long senderId);
    void clearChat(Long user1, Long user2);
    void deletePersonalChatForUser(Long userId, Long partnerId);
    ChatMessage findById(Long messageId);
    void initializePersonalChat(Long senderId, Long recipientId) throws AccessDeniedException;
    ChatMessageDto convertToDto(Long messageId);
    List<ChatMessageDto> convertToDtoList(List<Long> messageIds);
    ChatPhotoPageDto getPersonalChatPhotos(Long senderId, Long recipientId, int page, int size);
    ChatPhotoPageDto getPersonalChatFiles(Long senderId, Long recipientId, int page, int size);
    ChatPhotoPageDto getPersonalChatVoices(Long senderId, Long recipientId, int page, int size);
    void assertCanSignal(Long userId, Long chatId, Long recipientId) throws AccessDeniedException;
}
