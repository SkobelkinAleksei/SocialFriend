package org.example.chat.service;

import org.example.chat.entity.ChatMessageDto;
import org.example.chat.entity.ChatPollDto;
import org.example.chat.entity.CreatePollRequest;
import org.example.chat.entity.PollVoteRequest;
import com.example.common.dto.scheduler.KeepChatPollRequest;

import java.nio.file.AccessDeniedException;
import java.util.Collection;
import java.util.Map;

public interface ChatPollService {
    ChatMessageDto createPoll(Long chatId, Long userId, String firstName, String lastName, CreatePollRequest request)
            throws AccessDeniedException;

    ChatMessageDto createKeepChatPoll(Long chatId, KeepChatPollRequest request);

    boolean resolveKeepChatPoll(Long chatId);

    ChatMessageDto vote(Long pollId, Long userId, PollVoteRequest request) throws AccessDeniedException;

    ChatMessageDto close(Long pollId, Long userId) throws AccessDeniedException;

    Map<Long, ChatPollDto> mapByMessageIds(Collection<Long> messageIds, Long viewerId);
}
