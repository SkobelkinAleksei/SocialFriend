package org.example.chat.repository;

import org.example.chat.entity.ChatPoll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatPollRepository extends JpaRepository<ChatPoll, Long> {
    Optional<ChatPoll> findByMessageId(Long messageId);

    @Query("SELECT DISTINCT p FROM ChatPoll p LEFT JOIN FETCH p.options WHERE p.messageId IN :ids")
    List<ChatPoll> findByMessageIdIn(@Param("ids") Collection<Long> ids);

    Optional<ChatPoll> findFirstByChatIdAndPurpose(Long chatId, org.example.chat.entity.ChatPollPurpose purpose);

    List<ChatPoll> findByChatId(Long chatId);
}
