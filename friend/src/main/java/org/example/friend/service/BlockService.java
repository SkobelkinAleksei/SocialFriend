package org.example.friend.service;

import com.example.common.dto.BlockStatusDto;

import java.util.Set;

public interface BlockService {
    void block(Long authorId, Long blockedUserId);
    void unblock(Long authorId, Long blockedUserId);
    BlockStatusDto status(Long viewerId, Long targetUserId);
    boolean isBlockedEitherWay(Long userId1, Long userId2);
    Set<Long> hiddenUserIds(Long userId);
}
