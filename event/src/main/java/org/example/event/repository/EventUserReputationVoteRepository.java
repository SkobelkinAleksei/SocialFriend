package org.example.event.repository;

import org.example.event.entity.EventUserReputationVoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventUserReputationVoteRepository extends JpaRepository<EventUserReputationVoteEntity, Long> {

    // Найти все оценки, которые поставил конкретный пользователь (voterId) в рамках одного события
    List<EventUserReputationVoteEntity> findAllByEventIdAndVoterId(Long eventId, Long voterId);

    // поиск существующей оценки
    Optional<EventUserReputationVoteEntity> findByEventIdAndVoterIdAndTargetId(Long eventId, Long voterId, Long targetId);
}