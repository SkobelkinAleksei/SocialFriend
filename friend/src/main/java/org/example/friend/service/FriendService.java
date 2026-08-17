package org.example.friend.service;

import org.example.friend.dto.FriendDto;

import java.util.List;

public interface FriendService {
    List<FriendDto> findAllFriendsByUserId(Long userId, int page, int size);
    Boolean areFriends(Long userId1, Long userId2);
    long countFriends(Long userId);
    List<Long> findFriendIds(Long userId);
}
