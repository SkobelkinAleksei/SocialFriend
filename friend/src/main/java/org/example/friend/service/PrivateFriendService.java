package org.example.friend.service;

import org.example.friend.entity.FriendEntity;

public interface PrivateFriendService {
    void deleteFriendById(Long currentUserId, Long userId2);
    void createFriendship(FriendEntity friendEntity);
}
