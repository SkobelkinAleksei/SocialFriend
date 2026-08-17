package org.example.chat.service;

import lombok.RequiredArgsConstructor;
import org.example.chat.entity.ChatRoom;
import org.example.chat.entity.ChatRoomType;
import org.example.chat.entity.ChatUserPref;
import org.example.chat.repository.ChatUserPrefRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatPreferenceService {
    public static final int MAX_PINNED_CHATS = 5;

    private final ChatUserPrefRepository prefRepository;
    private final ChatAccess chatAccess;

    public Map<String, ChatUserPref> mapForUser(Long userId, List<String> scopeKeys) {
        Map<String, ChatUserPref> map = new HashMap<>();
        if (userId == null || scopeKeys == null || scopeKeys.isEmpty()) {
            return map;
        }
        for (ChatUserPref pref : prefRepository.findByUserIdAndScopeKeyIn(userId, scopeKeys)) {
            map.put(pref.getScopeKey(), pref);
        }
        return map;
    }

    public boolean isMuted(Long userId, String scopeKey) {
        return prefRepository.findByUserIdAndScopeKey(userId, scopeKey)
                .map(ChatUserPref::isMuted)
                .orElse(false);
    }

    @Transactional
    public ChatUserPref setMuted(Long userId, String scopeKey, boolean muted) {
        requireScopeAccess(userId, scopeKey);
        ChatUserPref pref = getOrCreate(userId, scopeKey);
        pref.setMuted(muted);
        return prefRepository.save(pref);
    }

    @Transactional
    public ChatUserPref pinPersonalChat(Long userId, Long partnerId) {
        if (partnerId == null || partnerId.equals(userId)) {
            throw new IllegalArgumentException("Нельзя закрепить этот чат");
        }
        return pinScope(userId, ChatAccess.personalScope(partnerId));
    }

    @Transactional
    public ChatUserPref pinRoom(Long userId, Long chatId) {
        ChatRoom room = chatAccess.requireRoom(chatId);
        chatAccess.requireParticipant(chatId, userId);
        if (room.getRoomType() != ChatRoomType.PERSONAL_GROUP) {
            throw new IllegalArgumentException("Закреплять можно только чаты из раздела «Личные»");
        }
        return pinScope(userId, ChatAccess.roomScope(chatId));
    }

    @Transactional
    public ChatUserPref unpin(Long userId, String scopeKey) {
        requireScopeAccess(userId, scopeKey);
        ChatUserPref pref = getOrCreate(userId, scopeKey);
        pref.setPinnedAt(null);
        return prefRepository.save(pref);
    }

    private void requireScopeAccess(Long userId, String scopeKey) {
        if (scopeKey != null && scopeKey.startsWith("R:")) {
            chatAccess.requireParticipant(Long.valueOf(scopeKey.substring(2)), userId);
        }
    }

    private ChatUserPref pinScope(Long userId, String scopeKey) {
        ChatUserPref pref = getOrCreate(userId, scopeKey);
        if (pref.getPinnedAt() != null) {
            return pref;
        }
        long pinned = prefRepository.findByUserIdAndPinnedAtIsNotNullOrderByPinnedAtAsc(userId).stream()
                .filter(item -> item.getScopeKey().startsWith("P:") || item.getScopeKey().startsWith("R:"))
                .count();
        if (pinned >= MAX_PINNED_CHATS) {
            throw new IllegalArgumentException("Можно закрепить не больше " + MAX_PINNED_CHATS + " чатов");
        }
        pref.setPinnedAt(LocalDateTime.now());
        return prefRepository.save(pref);
    }

    @Transactional
    public void deleteRoomPrefs(Long chatId) {
        prefRepository.deleteByScopeKey(ChatAccess.roomScope(chatId));
    }

    private ChatUserPref getOrCreate(Long userId, String scopeKey) {
        return prefRepository.findByUserIdAndScopeKey(userId, scopeKey)
                .orElseGet(() -> ChatUserPref.builder()
                        .userId(userId)
                        .scopeKey(scopeKey)
                        .muted(false)
                        .build());
    }
}
