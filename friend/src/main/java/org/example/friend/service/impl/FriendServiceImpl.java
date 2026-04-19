package org.example.friend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.friend.dto.FriendDto;
import org.example.friend.entity.FriendEntity;
import org.example.friend.mapper.FriendMapper;
import org.example.friend.repository.FriendRepository;
import org.example.friend.service.FriendService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class FriendServiceImpl implements FriendService {
    private final FriendRepository friendRepository;
    private final FriendMapper friendMapper;

    @Override
    @Transactional(readOnly = true)
    public List<FriendDto> findAllFriendsByUserId(Long userId) {
        List<FriendEntity> allFriend = friendRepository.findAllByUserId(userId);

        return allFriend.stream()
                .map(friendMapper::toDto)
                .toList();
    }
}
