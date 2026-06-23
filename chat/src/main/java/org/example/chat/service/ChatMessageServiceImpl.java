package org.example.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chat.entity.ChatMessage;
import org.example.chat.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageServiceImpl implements ChatMessageService {
    private final ChatMessageRepository messageRepository;

    @Override
    @Transactional
    public List<Long> getDistinctChatPartnerIds(Long userId) {
        // Вызываем метод репозитория, который найдет всех получателей и отправителей
        List<Long> recipients = messageRepository.findRecipientIdsBySenderId(userId);
        List<Long> senders = messageRepository.findSenderIdsByRecipientId(userId);

        // Объединяем списки и оставляем только уникальные ID (исключая самого себя)
        Set<Long> partnerIds = new HashSet<>();
        partnerIds.addAll(recipients);
        partnerIds.addAll(senders);
        partnerIds.remove(userId);

        return new ArrayList<>(partnerIds);
    }
    @Override
    @Transactional
    public ChatMessage saveMessage(ChatMessage chatMessage) {
        chatMessage.setTimestamp(LocalDateTime.now());
        return messageRepository.save(chatMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> getChatHistory(Long senderId, Long recipientId) {
        return messageRepository.findChatHistory(senderId, recipientId);
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

        // Проверяем, что редактировать пытается именно автор сообщения
        if (!message.getSenderId().equals(senderId)) {
            throw new SecurityException("Вы можете редактировать только свои сообщения");
        }

        message.setContent(newContent);
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

        messageRepository.delete(message);
    }

    @Override
    @Transactional
    public void clearChat(Long user1, Long user2) {
        messageRepository.deleteChatHistory(user1, user2);
    }

}
