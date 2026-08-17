package org.example.chat.repository;

import org.example.chat.entity.ChatPollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ChatPollVoteRepository extends JpaRepository<ChatPollVote, Long> {
    List<ChatPollVote> findByPollId(Long pollId);
    List<ChatPollVote> findByPollIdIn(Collection<Long> pollIds);
    List<ChatPollVote> findByPollIdAndUserId(Long pollId, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ChatPollVote v WHERE v.pollId = :pollId AND v.userId = :userId")
    void deleteByPollIdAndUserId(@Param("pollId") Long pollId, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ChatPollVote v WHERE v.pollId = :pollId AND v.userId = :userId AND v.optionId = :optionId")
    void deleteByPollIdAndUserIdAndOptionId(
            @Param("pollId") Long pollId,
            @Param("userId") Long userId,
            @Param("optionId") Long optionId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ChatPollVote v WHERE v.pollId = :pollId")
    void deleteByPollId(@Param("pollId") Long pollId);
}
