package org.example.friend.service;

import org.example.friend.dto.FriendDto;

import java.util.List;

public interface FriendService {
    List<FriendDto> findAllFriendsByUserId(Long userId);
}
