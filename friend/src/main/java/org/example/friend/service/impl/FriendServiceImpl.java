package org.example.friend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.friend.dto.FriendDto;
import org.example.friend.entity.FriendEntity;
import org.example.friend.mapper.FriendMapper;
import org.example.friend.repository.FriendRepository;
import org.example.friend.service.FriendService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class FriendServiceImpl implements FriendService {
    private final FriendRepository friendRepository;
    private final FriendMapper friendMapper;

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private static int boundedPage(int page) {
        return Math.max(page, 0);
    }

    private static int boundedSize(int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendDto> findAllFriendsByUserId(Long userId, int page, int size) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(boundedPage(page), boundedSize(size));

        Slice<FriendEntity> friendSlice = friendRepository.findAllByUserId(userId, pageable);

        return friendSlice.getContent().stream()
                .map(friendMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countFriends(Long userId) {
        return friendRepository.countByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findFriendIds(Long userId) {
        return friendRepository.findFriendIdsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean areFriends(Long userId1, Long userId2) {
        long u1 = Math.min(userId1, userId2);
        long u2 = Math.max(userId1, userId2);

        return friendRepository.findEntityByUserId1AndUserId2(u1, u2).isPresent();
    }
}
