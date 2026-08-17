package org.example.chat.repository;

import org.example.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  // Запрос для выгрузки всей истории переписки между двумя пользователями (в обе стороны)
  @Query("SELECT m FROM ChatMessage m WHERE " +
          "(m.senderId = :u1 AND m.recipientId = :u2) OR " +
          "(m.senderId = :u2 AND m.recipientId = :u1) " +
          "ORDER BY m.timestamp ASC, m.id ASC")
  List<ChatMessage> findChatHistory(@Param("u1") Long user1, @Param("u2") Long user2);


  @Query("SELECT COUNT(DISTINCT m.senderId) FROM ChatMessage m WHERE m.recipientId = :recipientId AND m.read = false")
  long countUnreadMessages(@Param("recipientId") Long recipientId);

  @Modifying
  @Query("UPDATE ChatMessage m SET m.read = true WHERE m.senderId = :senderId AND m.recipientId = :recipientId AND m.read = false")
  void markMessagesAsRead(@Param("senderId") Long senderId, @Param("recipientId") Long recipientId);

  @Modifying
  @Query("DELETE FROM ChatMessage m WHERE " +
          "(m.senderId = :u1 AND m.recipientId = :u2) OR " +
          "(m.senderId = :u2 AND m.recipientId = :u1)")
  void deleteChatHistory(@Param("u1") Long user1, @Param("u2") Long user2);

  @Query("SELECT DISTINCT m.recipientId FROM ChatMessage m WHERE m.senderId = :userId")
  List<Long> findRecipientIdsBySenderId(@Param("userId") Long userId);

  @Query("SELECT DISTINCT m.senderId FROM ChatMessage m WHERE m.recipientId = :userId")
  List<Long> findSenderIdsByRecipientId(@Param("userId") Long userId);

  @Query("SELECT DISTINCT m FROM ChatMessage m " +
          "LEFT JOIN FETCH m.replies " +
          "WHERE m.chatId = :chatId " +
          "ORDER BY m.timestamp ASC")
  List<ChatMessage> findGroupChatHistory(@Param("chatId") Long chatId);

  @Query("SELECT m FROM ChatMessage m " +
          "LEFT JOIN FETCH m.forwardedFrom " +
          "LEFT JOIN FETCH m.bundledForwards " +
          "WHERE m.id = :id")
  Optional<ChatMessage> findByIdWithForwardedFrom(@Param("id") Long id);

  @Query("SELECT COALESCE(MAX(m.id), 0) FROM ChatMessage m WHERE " +
          "((m.senderId = :u1 AND m.recipientId = :u2) OR (m.senderId = :u2 AND m.recipientId = :u1)) " +
          "AND (m.content = '[CHAT_HISTORY_CLEARED]' OR m.content = CONCAT('[CHAT_HISTORY_CLEARED:', :userId, ']'))")
  Long findPersonalClearCutId(@Param("u1") Long user1, @Param("u2") Long user2, @Param("userId") Long userId);

  @Query("SELECT m.id FROM ChatMessage m WHERE " +
          "((m.senderId = :u1 AND m.recipientId = :u2) OR (m.senderId = :u2 AND m.recipientId = :u1)) " +
          "AND m.id > :minId AND m.id < :beforeId " +
          "AND (m.content IS NULL OR (" +
          "m.content <> '[CHAT_HISTORY_CLEARED]' AND m.content NOT LIKE '[CHAT_HISTORY_CLEARED:%' AND " +
          "m.content <> '[CHAT_DELETED_BY_USER]' AND m.content NOT LIKE '[CHAT_DELETED_BY_USER:%')) " +
          "ORDER BY m.id DESC")
  List<Long> findPersonalHistoryPageIds(
          @Param("u1") Long user1,
          @Param("u2") Long user2,
          @Param("minId") Long minId,
          @Param("beforeId") Long beforeId,
          org.springframework.data.domain.Pageable pageable);

  @Query("SELECT COALESCE(MAX(m.id), 0) FROM ChatMessage m WHERE m.chatId = :chatId " +
          "AND m.content = CONCAT('[GROUP_CHAT_HISTORY_CLEARED:', :userId, ']')")
  Long findGroupClearCutId(@Param("chatId") Long chatId, @Param("userId") Long userId);

  @Query("SELECT m.id FROM ChatMessage m WHERE m.chatId = :chatId " +
          "AND m.id > :minId AND m.id < :beforeId " +
          "AND (m.content IS NULL OR m.content NOT LIKE '%[GROUP_CHAT_HISTORY_CLEARED:%') " +
          "ORDER BY m.id DESC")
  List<Long> findGroupHistoryPageIds(
          @Param("chatId") Long chatId,
          @Param("minId") Long minId,
          @Param("beforeId") Long beforeId,
          org.springframework.data.domain.Pageable pageable);

  boolean existsByChatIdAndContent(Long chatId, String content);

    List<ChatMessage> findByChatId(Long chatId);

    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.chatId = :chatId AND m.id >= :fromId AND m.id <= :toId
            ORDER BY m.id ASC
            """)
    List<ChatMessage> findGroupBetween(
            @Param("chatId") Long chatId,
            @Param("fromId") Long fromId,
            @Param("toId") Long toId
    );

    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.chatId = :chatId AND m.id < :messageId
            ORDER BY m.id DESC
            """)
    List<ChatMessage> findGroupBefore(
            @Param("chatId") Long chatId,
            @Param("messageId") Long messageId,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.chatId = :chatId AND m.id > :messageId
            ORDER BY m.id ASC
            """)
    List<ChatMessage> findGroupAfter(
            @Param("chatId") Long chatId,
            @Param("messageId") Long messageId,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.chatId IS NULL
              AND ((m.senderId = :u1 AND m.recipientId = :u2) OR (m.senderId = :u2 AND m.recipientId = :u1))
              AND m.id >= :fromId AND m.id <= :toId
            ORDER BY m.id ASC
            """)
    List<ChatMessage> findPersonalBetween(
            @Param("u1") Long user1,
            @Param("u2") Long user2,
            @Param("fromId") Long fromId,
            @Param("toId") Long toId
    );

    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.chatId IS NULL
              AND ((m.senderId = :u1 AND m.recipientId = :u2) OR (m.senderId = :u2 AND m.recipientId = :u1))
              AND m.id < :messageId
            ORDER BY m.id DESC
            """)
    List<ChatMessage> findPersonalBefore(
            @Param("u1") Long user1,
            @Param("u2") Long user2,
            @Param("messageId") Long messageId,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.chatId IS NULL
              AND ((m.senderId = :u1 AND m.recipientId = :u2) OR (m.senderId = :u2 AND m.recipientId = :u1))
              AND m.id > :messageId
            ORDER BY m.id ASC
            """)
    List<ChatMessage> findPersonalAfter(
            @Param("u1") Long user1,
            @Param("u2") Long user2,
            @Param("messageId") Long messageId,
            org.springframework.data.domain.Pageable pageable
    );
}