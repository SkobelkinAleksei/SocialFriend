package org.example.chat.repository;

import org.example.chat.entity.ChatPinnedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatPinnedMessageRepository extends JpaRepository<ChatPinnedMessage, Long> {
    Optional<ChatPinnedMessage> findByMessageId(Long messageId);

    List<ChatPinnedMessage> findByChatIdOrderByPinnedAtDesc(Long chatId);

    List<ChatPinnedMessage> findByDmUserMinAndDmUserMaxOrderByPinnedAtDesc(Long dmUserMin, Long dmUserMax);

    List<ChatPinnedMessage> findByMessageIdIn(Collection<Long> messageIds);

    void deleteByMessageId(Long messageId);

    void deleteByChatId(Long chatId);
}
