package org.example.friend.service.impl;

import com.example.common.dto.BlockStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.friend.entity.FriendBlockListEntity;
import org.example.friend.repository.FriendBlockListRepository;
import org.example.friend.repository.FriendRepository;
import org.example.friend.repository.FriendRequestRepository;
import org.example.friend.service.BlockService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockServiceImpl implements BlockService {

    private final FriendBlockListRepository blockListRepository;
    private final FriendRepository friendRepository;
    private final FriendRequestRepository friendRequestRepository;

    @Override
    @Transactional
    public void block(Long authorId, Long blockedUserId) {
        if (authorId == null || blockedUserId == null) {
            throw new IllegalArgumentException("Не указан пользователь");
        }
        if (authorId.equals(blockedUserId)) {
            throw new IllegalArgumentException("Нельзя заблокировать самого себя");
        }
        if (blockListRepository.existsByAuthorIdAndBlockedUserId(authorId, blockedUserId)) {
            return;
        }
        try {
            blockListRepository.saveAndFlush(FriendBlockListEntity.builder()
                    .authorId(authorId)
                    .blockedUserId(blockedUserId)
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.info("[Block] Повторная блокировка {} → {} — skip", authorId, blockedUserId);
        }
        breakFriendship(authorId, blockedUserId);
        friendRequestRepository.findRequestBetweenUsers(authorId, blockedUserId)
                .ifPresent(friendRequestRepository::delete);
        log.info("[Block] {} заблокировал {}", authorId, blockedUserId);
    }

    @Override
    @Transactional
    public void unblock(Long authorId, Long blockedUserId) {
        if (authorId == null || blockedUserId == null) {
            throw new IllegalArgumentException("Не указан пользователь");
        }
        blockListRepository.deleteByAuthorIdAndBlockedUserId(authorId, blockedUserId);
        log.info("[Block] {} разблокировал {}", authorId, blockedUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public BlockStatusDto status(Long viewerId, Long targetUserId) {
        if (viewerId == null || targetUserId == null || viewerId.equals(targetUserId)) {
            return new BlockStatusDto(false, false);
        }
        boolean blockedByMe = blockListRepository.existsByAuthorIdAndBlockedUserId(viewerId, targetUserId);
        boolean blockedMe = blockListRepository.existsByAuthorIdAndBlockedUserId(targetUserId, viewerId);
        return new BlockStatusDto(blockedByMe, blockedMe);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlockedEitherWay(Long userId1, Long userId2) {
        return status(userId1, userId2).isEitherWay();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> hiddenUserIds(Long userId) {
        Set<Long> ids = new HashSet<>();
        if (userId == null) {
            return ids;
        }
        blockListRepository.findByAuthorId(userId)
                .forEach(row -> ids.add(row.getBlockedUserId()));
        blockListRepository.findByBlockedUserId(userId)
                .forEach(row -> ids.add(row.getAuthorId()));
        ids.remove(userId);
        return ids;
    }

    private void breakFriendship(Long userId1, Long userId2) {
        long a = Math.min(userId1, userId2);
        long b = Math.max(userId1, userId2);
        friendRepository.findEntityByUserId1AndUserId2(a, b)
                .ifPresent(friend -> friendRepository.deleteById(friend.getId()));
    }
}
