package org.example.chat.service;

import org.example.chat.entity.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ChatMessageService {
    List<Long> getDistinctChatPartnerIds(Long userId);
    // Сохранение нового сообщения
    ChatMessage saveMessage(ChatMessage chatMessage);

    // Получение истории переписки между двумя пользователями
    List<ChatMessage> getChatHistory(Long senderId, Long recipientId);

    long getUnreadCount(Long recipientId);

    void readAllMessagesFromUser(Long senderId, Long recipientId);

    ChatMessage editMessage(Long messageId, String newContent, Long senderId);
    void deleteMessage(Long messageId, Long senderId);
    void clearChat(Long user1, Long user2);
}
