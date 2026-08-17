package org.example.scheduler.service;

import com.example.common.dto.scheduler.EventLifecycleRequest;
import com.example.common.dto.scheduler.EventLifecycleSnapshot;
import com.example.common.dto.scheduler.KeepChatPollRequest;
import com.example.common.kafka.NotificationType;
import com.example.common.lifecycle.EventLifecycleSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.scheduler.client.ChatInternalClient;
import org.example.scheduler.client.EventInternalClient;
import org.example.scheduler.client.SecurityInternalClient;
import org.example.scheduler.entity.ScheduledJob;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutor {

    public static final String MSG_REPUTATION_OPEN = "Теперь можно поставить оценку участникам встречи";
    public static final String MSG_REPUTATION_CLOSED = "Окно оценок за эту встречу закрыто";
    public static final String MSG_KEEP_CHAT_SAVED =
            "Большинство проголосовало за сохранение чата — он остаётся. Если чат вам больше не нужен, вы можете из него выйти.";

    private final EventInternalClient eventClient;
    private final ChatInternalClient chatClient;
    private final SecurityInternalClient securityClient;
    private final ObjectMapper objectMapper;
    private final EventLifecycleSettings lifecycleSettings;
    private final JobScheduleService jobScheduleService;

    public void execute(ScheduledJob job) throws Exception {
        switch (job.getType()) {
            case EVENT_REMIND_1H -> remind(job);
            case EVENT_STARTED -> started(job);
            case EVENT_OPEN_REPUTATION -> openReputation(job);
            case EVENT_KEEP_CHAT_POLL -> keepPoll(job);
            case EVENT_RESOLVE_KEEP_CHAT -> resolveKeep(job);
            case EVENT_CLOSE_REPUTATION -> closeReputation(job);
            case EVENT_DELETE_CHAT_AFTER_CANCEL, EVENT_DELETE_CHAT_AFTER_KEEP_FAIL -> chatClient.deleteRoom(eventId(job));
            case REFRESH_TOKEN_CLEANUP -> securityClient.cleanupExpiredRefreshTokens();
        }
    }

    private void remind(ScheduledJob job) throws Exception {
        EventLifecycleSnapshot snapshot = eventClient.snapshot(eventId(job));
        if (!snapshot.isExists() || snapshot.isStarted()) {
            return;
        }
        chatClient.postSystemMessage(snapshot.getEventId(),
                "Встреча начнётся через " + lifecycleSettings.minutesLabel(lifecycleSettings.getRemindMinutesBefore()));
        eventClient.notifyParticipants(snapshot.getEventId(), NotificationType.EVENT_REMIND_1H);
    }

    private void started(ScheduledJob job) throws Exception {
        EventLifecycleSnapshot snapshot = eventClient.markStarted(eventId(job));
        if (!snapshot.isExists()) {
            return;
        }
        if (snapshot.getPeopleCount() < lifecycleSettings.getMinPeople()) {
            chatClient.closeRoom(snapshot.getEventId());
        }
    }

    private void openReputation(ScheduledJob job) throws Exception {
        EventLifecycleSnapshot snapshot = eventClient.snapshot(eventId(job));
        if (!snapshot.isExists()) {
            return;
        }
        chatClient.postSystemMessage(snapshot.getEventId(), MSG_REPUTATION_OPEN);
        eventClient.notifyParticipants(snapshot.getEventId(), NotificationType.EVENT_REPUTATION_OPEN);
    }

    private void keepPoll(ScheduledJob job) throws Exception {
        EventLifecycleSnapshot snapshot = eventClient.snapshot(eventId(job));
        if (!snapshot.isExists()) {
            return;
        }
        if (snapshot.getPeopleCount() < lifecycleSettings.getMinPeople()) {
            chatClient.closeRoom(snapshot.getEventId());
            return;
        }
        chatClient.createKeepPoll(snapshot.getEventId(), KeepChatPollRequest.builder()
                .eligibleVoterIds(snapshot.getEligibleVoterIds())
                .closesAt(lifecycleSettings.keepResolveAt(snapshot.getEventDate()))
                .build());
        eventClient.notifyParticipants(snapshot.getEventId(), NotificationType.EVENT_CHAT_KEEP_VOTE);
    }

    private void resolveKeep(ScheduledJob job) throws Exception {
        Long id = eventId(job);
        boolean keep = chatClient.resolveKeepPoll(id);
        if (keep) {
            chatClient.postSystemMessage(id, MSG_KEEP_CHAT_SAVED);
            return;
        }
        chatClient.postSystemMessage(id, keepFailMessage());
        jobScheduleService.scheduleKeepFailDelete(id);
    }

    private String keepFailMessage() {
        return "Не набралось " + lifecycleSettings.getKeepYesPercent()
                + "% голосов «за». Чат будет удалён через "
                + lifecycleSettings.minutesLabel(lifecycleSettings.getKeepFailDeleteMinutes()) + ".";
    }

    private void closeReputation(ScheduledJob job) throws Exception {
        EventLifecycleSnapshot snapshot = eventClient.snapshot(eventId(job));
        if (!snapshot.isExists()) {
            return;
        }
        chatClient.postSystemMessage(snapshot.getEventId(), MSG_REPUTATION_CLOSED);
    }

    private Long eventId(ScheduledJob job) throws Exception {
        EventLifecycleRequest request = objectMapper.readValue(job.getPayload(), EventLifecycleRequest.class);
        if (request.getEventId() == null) {
            throw new IllegalArgumentException("В задаче нет eventId");
        }
        return request.getEventId();
    }
}
