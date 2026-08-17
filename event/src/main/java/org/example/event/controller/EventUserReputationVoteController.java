package org.example.event.controller; // или ваш пакет контроллеров

import org.example.event.dto.VoteDto;
import org.example.event.entity.enums.VoteType;
import org.example.event.service.EventUserReputationVoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/social/events/reputation")
@Slf4j
@RequiredArgsConstructor
public class EventUserReputationVoteController {

    private final EventUserReputationVoteService reputationVoteService;

    // 1. Принять голос за участника встречи
    @PostMapping("/vote")
    public ResponseEntity<Void> vote(
            @RequestParam Long eventId,
            @RequestParam Long targetId,
            @RequestParam VoteType voteType,
            @RequestHeader("X-User-Id") Long currentUserId) {

        log.info("[REST] Запрос оценки от пользователя {} для {} ({})", currentUserId, targetId, voteType);
        reputationVoteService.vote(eventId, currentUserId, targetId, voteType);
        return ResponseEntity.ok().build();
    }

    // 2. Получить список прошлых голосов текущего юзера в рамках одного прошедшего события
    @GetMapping("/my-votes/{eventId}")
    public ResponseEntity<List<VoteDto>> getMyVotes(
            @PathVariable Long eventId,
            @RequestHeader("X-User-Id") Long currentUserId) {

        log.info("[REST] Запрос истории голосов пользователя {} в событии {}", currentUserId, eventId);
        List<VoteDto> myVotes = reputationVoteService.getMyVotesInEvent(eventId, currentUserId)
                .stream()
                .map(v -> new VoteDto(v.getTargetId(), v.getVoteType()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(myVotes);
    }
}
