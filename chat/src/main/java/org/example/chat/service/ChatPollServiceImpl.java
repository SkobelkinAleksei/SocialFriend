package org.example.chat.service;

import com.example.common.RequestData;
import com.example.common.dto.event.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chat.entity.ChatMessage;
import org.example.chat.entity.ChatMessageDto;
import org.example.chat.entity.ChatPoll;
import org.example.chat.entity.ChatPollDto;
import org.example.chat.entity.ChatPollOption;
import org.example.chat.entity.ChatPollVote;
import com.example.common.dto.scheduler.KeepChatPollRequest;
import com.example.common.lifecycle.EventLifecycleSettings;
import org.example.chat.entity.ChatPollPurpose;
import org.example.chat.entity.ChatSystemMessages;
import org.example.chat.entity.CreatePollRequest;
import org.example.chat.entity.PollVoteRequest;
import org.example.chat.repository.ChatParticipantRepository;
import org.example.chat.repository.ChatPollRepository;
import org.example.chat.repository.ChatPollVoteRepository;
import org.example.restclient.config.IHttpCore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatPollServiceImpl implements ChatPollService {

    private final ChatPollRepository pollRepository;
    private final ChatPollVoteRepository voteRepository;
    private final ChatParticipantRepository participantRepository;
    private final ChatMessageService messageService;
    private final IHttpCore httpCore;
    private final EventLifecycleSettings lifecycleSettings;

    @Value("${app.services.user-base-url:http://localhost:8081}")
    private String userBaseUrl;

    @Override
    @Transactional
    public ChatMessageDto createPoll(Long chatId, Long userId, String firstName, String lastName, CreatePollRequest request)
            throws AccessDeniedException {
        assertParticipant(chatId, userId);
        List<String> options = normalizeOptions(request.getOptions());
        String question = request.getQuestion() == null ? "" : request.getQuestion().trim();
        if (question.isEmpty()) {
            throw new IllegalArgumentException("Введите вопрос голосования");
        }
        if (question.length() > 255) {
            throw new IllegalArgumentException("Вопрос не длиннее 255 символов");
        }
        int filledOptions = 0;
        if (request.getOptions() != null) {
            filledOptions = (int) request.getOptions().stream()
                    .filter(option -> option != null && !option.trim().isEmpty())
                    .count();
        }
        if (options.size() < 2) {
            if (filledOptions >= 2) {
                throw new IllegalArgumentException("Варианты не должны повторяться. Укажите минимум два разных варианта");
            }
            throw new IllegalArgumentException("Нужно минимум два варианта ответа");
        }

        ChatMessage saved = messageService.saveMessage(ChatMessage.builder()
                .chatId(chatId)
                .senderId(userId)
                .senderFirstName(firstName)
                .senderLastName(lastName)
                .content("Голосование: " + question)
                .timestamp(LocalDateTime.now())
                .build());

        ChatPoll poll = ChatPoll.builder()
                .messageId(saved.getId())
                .chatId(chatId)
                .creatorId(userId)
                .question(question)
                .anonymous(request.isAnonymous())
                .multiple(request.isMultiple())
                .closed(false)
                .options(new ArrayList<>())
                .build();
        for (int i = 0; i < options.size(); i++) {
            poll.getOptions().add(ChatPollOption.builder()
                    .poll(poll)
                    .text(options.get(i))
                    .position(i)
                    .build());
        }
        poll = pollRepository.save(poll);

        ChatMessageDto dto = messageService.convertToDto(saved.getId());
        dto.setPoll(toDto(poll, List.of(), userId, Map.of()));
        return dto;
    }

    @Override
    @Transactional
    public ChatMessageDto createKeepChatPoll(Long chatId, KeepChatPollRequest request) {
        Optional<ChatPoll> existing = pollRepository.findFirstByChatIdAndPurpose(chatId, ChatPollPurpose.KEEP_CHAT);
        if (existing.isPresent()) {
            return dtoWithPoll(existing.get().getMessageId(), existing.get().getId(), 0L, false);
        }
        ChatMessage saved;
        try {
            saved = messageService.saveMessage(ChatMessage.builder()
                    .chatId(chatId)
                    .senderId(0L)
                    .content("Голосование за сохранение чата")
                    .timestamp(LocalDateTime.now())
                    .isSystem(true)
                    .build());
        } catch (AccessDeniedException e) {
            throw new IllegalStateException("Не удалось создать системное голосование", e);
        }
        participantRepository.incrementUnreadCountForAndAllByChatId(chatId);

        ChatPoll poll = ChatPoll.builder()
                .messageId(saved.getId())
                .chatId(chatId)
                .creatorId(0L)
                .question(ChatSystemMessages.KEEP_CHAT_QUESTION)
                .anonymous(false)
                .multiple(false)
                .closed(false)
                .purpose(ChatPollPurpose.KEEP_CHAT)
                .closesAt(request != null ? request.getClosesAt() : null)
                .eligibleVoterIds(new HashSet<>(request != null && request.getEligibleVoterIds() != null
                        ? request.getEligibleVoterIds() : List.of()))
                .options(new ArrayList<>())
                .build();
        poll.getOptions().add(ChatPollOption.builder()
                .poll(poll)
                .text(ChatSystemMessages.KEEP_CHAT_YES)
                .position(0)
                .build());
        poll.getOptions().add(ChatPollOption.builder()
                .poll(poll)
                .text(ChatSystemMessages.KEEP_CHAT_NO)
                .position(1)
                .build());
        poll = pollRepository.save(poll);

        ChatMessageDto dto = messageService.convertToDto(saved.getId());
        dto.setPoll(toDto(poll, List.of(), 0L, Map.of()));
        dto.setSystem(true);
        return dto;
    }

    @Override
    @Transactional
    public boolean resolveKeepChatPoll(Long chatId) {
        ChatPoll poll = pollRepository.findFirstByChatIdAndPurpose(chatId, ChatPollPurpose.KEEP_CHAT).orElse(null);
        if (poll == null) {
            return true;
        }
        poll.setClosed(true);
        pollRepository.save(poll);
        List<ChatPollVote> votes = voteRepository.findByPollId(poll.getId());
        Long yesOptionId = poll.getOptions().stream()
                .filter(option -> option.getPosition() == 0)
                .map(ChatPollOption::getId)
                .findFirst()
                .orElse(null);
        int yesCount = (int) votes.stream().filter(vote -> vote.getOptionId().equals(yesOptionId)).count();
        int eligible = poll.getEligibleVoterIds() == null || poll.getEligibleVoterIds().isEmpty()
                ? 0
                : poll.getEligibleVoterIds().size();
        return lifecycleSettings.keepChat(yesCount, eligible);
    }

    @Override
    @Transactional
    public ChatMessageDto vote(Long pollId, Long userId, PollVoteRequest request) throws AccessDeniedException {
        ChatPoll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("Голосование не найдено"));
        assertParticipant(poll.getChatId(), userId);
        if (poll.getClosesAt() != null && !LocalDateTime.now().isBefore(poll.getClosesAt())) {
            poll.setClosed(true);
            pollRepository.save(poll);
        }
        if (poll.isClosed()) {
            throw new IllegalArgumentException("Голосование уже завершено");
        }
        if (poll.getPurpose() == ChatPollPurpose.KEEP_CHAT
                && (poll.getEligibleVoterIds() == null || !poll.getEligibleVoterIds().contains(userId))) {
            throw new AccessDeniedException("Вы не можете голосовать в этом опросе");
        }

        Set<Long> optionIds = poll.getOptions().stream().map(ChatPollOption::getId).collect(Collectors.toSet());
        List<Long> requested = request.getOptionIds() == null ? List.of() : request.getOptionIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (requested.stream().anyMatch(id -> !optionIds.contains(id))) {
            throw new IllegalArgumentException("Некорректный вариант ответа");
        }

        List<ChatPollVote> mine = voteRepository.findByPollIdAndUserId(pollId, userId);
        Set<Long> mineIds = mine.stream().map(ChatPollVote::getOptionId).collect(Collectors.toSet());

        if (poll.isMultiple()) {
            if (requested.size() != 1) {
                throw new IllegalArgumentException("Отметьте один вариант");
            }
            Long optionId = requested.get(0);
            if (mineIds.contains(optionId)) {
                voteRepository.deleteByPollIdAndUserIdAndOptionId(pollId, userId, optionId);
            } else {
                voteRepository.save(ChatPollVote.builder()
                        .pollId(pollId)
                        .optionId(optionId)
                        .userId(userId)
                        .build());
            }
        } else {
            Long optionId = requested.isEmpty() ? null : requested.get(0);
            if (optionId != null && mineIds.size() == 1 && mineIds.contains(optionId)) {
                voteRepository.deleteByPollIdAndUserId(pollId, userId);
            } else if (optionId != null) {
                voteRepository.deleteByPollIdAndUserId(pollId, userId);
                voteRepository.save(ChatPollVote.builder()
                        .pollId(pollId)
                        .optionId(optionId)
                        .userId(userId)
                        .build());
            }
        }

        return dtoWithPoll(poll.getMessageId(), poll.getId(), userId, true);
    }

    @Override
    @Transactional
    public ChatMessageDto close(Long pollId, Long userId) throws AccessDeniedException {
        ChatPoll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("Голосование не найдено"));
        assertParticipant(poll.getChatId(), userId);
        if (poll.getPurpose() == ChatPollPurpose.KEEP_CHAT) {
            throw new AccessDeniedException("Это голосование закрывается автоматически");
        }
        if (!userId.equals(poll.getCreatorId())) {
            throw new AccessDeniedException("Завершить голосование может только автор");
        }
        poll.setClosed(true);
        pollRepository.save(poll);
        return dtoWithPoll(poll.getMessageId(), poll.getId(), userId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ChatPollDto> mapByMessageIds(Collection<Long> messageIds, Long viewerId) {
        Map<Long, ChatPollDto> result = new HashMap<>();
        if (messageIds == null || messageIds.isEmpty()) {
            return result;
        }
        List<ChatPoll> polls = pollRepository.findByMessageIdIn(messageIds);
        if (polls.isEmpty()) {
            return result;
        }
        List<Long> pollIds = polls.stream().map(ChatPoll::getId).toList();
        List<ChatPollVote> allVotes = voteRepository.findByPollIdIn(pollIds);
        Map<Long, List<ChatPollVote>> votesByPoll = allVotes.stream()
                .collect(Collectors.groupingBy(ChatPollVote::getPollId));
        Map<Long, UserDto> users = fetchUsers(allVotes.stream().map(ChatPollVote::getUserId).collect(Collectors.toSet()));

        for (ChatPoll poll : polls) {
            result.put(poll.getMessageId(), toDto(poll, votesByPoll.getOrDefault(poll.getId(), List.of()), viewerId, users));
        }
        return result;
    }

    private ChatMessageDto dtoWithPoll(Long messageId, Long pollId, Long viewerId, boolean pollUpdate) {
        ChatPoll poll = pollRepository.findById(pollId).orElseThrow();
        List<ChatPollVote> votes = voteRepository.findByPollId(pollId);
        Map<Long, UserDto> users = fetchUsers(votes.stream().map(ChatPollVote::getUserId).collect(Collectors.toSet()));
        ChatMessageDto dto = messageService.convertToDto(messageId);
        dto.setPoll(toDto(poll, votes, viewerId, users));
        dto.setPollUpdate(pollUpdate);
        return dto;
    }

    private ChatPollDto toDto(ChatPoll poll, List<ChatPollVote> votes, Long viewerId, Map<Long, UserDto> users) {
        Map<Long, List<ChatPollVote>> byOption = votes.stream().collect(Collectors.groupingBy(ChatPollVote::getOptionId));
        Set<Long> myOptions = votes.stream()
                .filter(v -> viewerId != null && viewerId.equals(v.getUserId()))
                .map(ChatPollVote::getOptionId)
                .collect(Collectors.toSet());

        List<ChatPollDto.Option> options = new ArrayList<>();
        for (ChatPollOption option : poll.getOptions()) {
            List<ChatPollVote> optionVotes = byOption.getOrDefault(option.getId(), List.of());
            List<ChatPollDto.Voter> voters = new ArrayList<>();
            if (!poll.isAnonymous()) {
                for (ChatPollVote vote : optionVotes) {
                    UserDto user = users.get(vote.getUserId());
                    voters.add(ChatPollDto.Voter.builder()
                            .userId(vote.getUserId())
                            .firstName(user != null ? user.getFirstName() : "Участник")
                            .lastName(user != null ? user.getLastName() : "")
                            .avatarUrl(user != null ? user.getAvatarUrl() : null)
                            .build());
                }
            }
            options.add(ChatPollDto.Option.builder()
                    .id(option.getId())
                    .text(option.getText())
                    .votes(optionVotes.size())
                    .selected(myOptions.contains(option.getId()))
                    .voters(voters)
                    .build());
        }

        int total = poll.isMultiple()
                ? (int) votes.stream().map(ChatPollVote::getUserId).distinct().count()
                : votes.size();

        return ChatPollDto.builder()
                .id(poll.getId())
                .messageId(poll.getMessageId())
                .chatId(poll.getChatId())
                .creatorId(poll.getCreatorId())
                .question(poll.getQuestion())
                .anonymous(poll.isAnonymous())
                .multiple(poll.isMultiple())
                .closed(poll.isClosed())
                .purpose(poll.getPurpose() != null ? poll.getPurpose().name() : ChatPollPurpose.USER.name())
                .closesAt(poll.getClosesAt())
                .totalVotes(total)
                .options(options)
                .build();
    }

    private void assertParticipant(Long chatId, Long userId) throws AccessDeniedException {
        if (userId == null || participantRepository.findByChatIdAndUserId(chatId, userId).isEmpty()) {
            throw new AccessDeniedException("Вы не участник этого группового чата");
        }
    }

    private static List<String> normalizeOptions(List<String> raw) {
        if (raw == null) return List.of();
        List<String> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String option : raw) {
            if (option == null) continue;
            String text = option.trim();
            if (text.isEmpty() || text.length() > 100) continue;
            String key = text.toLowerCase();
            if (!seen.add(key)) continue;
            out.add(text);
            if (out.size() == 10) break;
        }
        return out;
    }

    private Map<Long, UserDto> fetchUsers(Set<Long> ids) {
        Map<Long, UserDto> result = new HashMap<>();
        if (ids == null || ids.isEmpty()) return result;
        try {
            String idsParam = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
            String url = userBaseUrl + "/api/v1/social/users/search/by-ids?ids=" + idsParam;
            ResponseEntity<UserDto[]> response = httpCore.get(new RequestData(url), UserDto[].class);
            if (response != null && response.getBody() != null) {
                for (UserDto user : response.getBody()) {
                    if (user.getUserId() != null) result.put(user.getUserId(), user);
                }
            }
        } catch (Exception e) {
            log.error("[Chat-Poll] Не удалось загрузить участников голосования", e);
        }
        return result;
    }
}
