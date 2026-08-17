package org.example.chat.repository;

import org.example.chat.entity.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    Optional<ChatParticipant> findByChatIdAndUserId(Long chatId, Long userId);

    List<ChatParticipant> findAllByChatId(Long chatId);

    List<ChatParticipant> findAllByUserId(Long userId);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE chat_participants SET unread_count = unread_count + 1 WHERE chat_id = :chatId", nativeQuery = true)
    void incrementUnreadCountForAndAllByChatId(@Param("chatId") Long chatId);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE chat_participants SET unread_count = unread_count + 1 WHERE chat_id = :chatId AND user_id <> :excludeUserId", nativeQuery = true)
    void incrementUnreadExcept(@Param("chatId") Long chatId, @Param("excludeUserId") Long excludeUserId);

    @Modifying
    @Query(value = "DELETE FROM chat_participants cp USING chat_rooms cr WHERE cp.chat_id = cr.id AND cr.event_id = :eventId AND cp.user_id = :userId", nativeQuery = true)
    void deleteByEventIdAndUserId(@Param("eventId") Long eventId, @Param("userId") Long userId);

    @Query("SELECT cp.userId FROM ChatParticipant cp WHERE cp.chat.id = :chatId")
    List<Long> findUserIdsByChatId(@Param("chatId") Long chatId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ChatParticipant cp WHERE cp.chat.id = :chatId")
    void deleteByChatId(@Param("chatId") Long chatId);
}
