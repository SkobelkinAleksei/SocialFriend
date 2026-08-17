package org.example.chat.repository;

import org.example.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByEventId(Long eventId);

    // Поиск всех групповых чатов, в которых состоит пользователь
    @Query("SELECT cp.chat FROM ChatParticipant cp WHERE cp.userId = :userId")
    List<ChatRoom> findAllRoomsByUserId(@Param("userId") Long userId);

    @Modifying
    @Query(value = "UPDATE chat_messages SET read = true WHERE chat_id = :chatId", nativeQuery = true)
    void markAllRoomMessagesAsRead(@Param("chatId") Long chatId);

    @Query("SELECT r FROM ChatRoom r " +
            "JOIN ChatParticipant cp ON cp.chat.id = r.id " +
            "WHERE cp.userId = :userId " +
            "ORDER BY (" +
            "  SELECT COALESCE(MAX(m.timestamp), r.createdAt) " +
            "  FROM ChatMessage m " +
            "  WHERE m.chatId = r.id" +
            ") DESC")
    List<ChatRoom> findAllRoomsByUserIdSortedByLatestMessage(@Param("userId") Long userId);

}