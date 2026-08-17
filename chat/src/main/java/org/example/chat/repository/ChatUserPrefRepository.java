package org.example.chat.repository;

import org.example.chat.entity.ChatUserPref;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatUserPrefRepository extends JpaRepository<ChatUserPref, Long> {
    Optional<ChatUserPref> findByUserIdAndScopeKey(Long userId, String scopeKey);

    List<ChatUserPref> findByUserIdAndScopeKeyIn(Long userId, Collection<String> scopeKeys);

    List<ChatUserPref> findByUserIdAndPinnedAtIsNotNullOrderByPinnedAtAsc(Long userId);

    void deleteByScopeKey(String scopeKey);
}
