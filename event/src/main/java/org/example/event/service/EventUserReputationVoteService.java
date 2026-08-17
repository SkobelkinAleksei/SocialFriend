package org.example.event.service;

import org.example.event.entity.EventUserReputationVoteEntity;
import org.example.event.entity.enums.VoteType;

import java.util.List;

public interface EventUserReputationVoteService {

    // Зафиксировать голос (плюс или минус) от одного участника другому
    void vote(Long eventId, Long voterId, Long targetId, VoteType voteType);

    // Получить список уже поставленных голосов текущего юзера на этой встрече (для фронтенда)
    List<EventUserReputationVoteEntity> getMyVotesInEvent(Long eventId, Long voterId);
}