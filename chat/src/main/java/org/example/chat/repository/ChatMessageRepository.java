package org.example.chat.repository;

import org.example.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  // Запрос для выгрузки всей истории переписки между двумя пользователями (в обе стороны)
  @Query("SELECT m FROM ChatMessage m WHERE " +
          "(m.senderId = :u1 AND m.recipientId = :u2) OR " +
          "(m.senderId = :u2 AND m.recipientId = :u1) " +
          "ORDER BY m.timestamp ASC")
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
}