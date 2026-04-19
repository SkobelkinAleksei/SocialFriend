package org.example.friend.service;

import org.example.friend.dto.FriendRequestDto;
import org.example.friend.entity.enums.FriendRequestStatus;
import org.example.friend.entity.enums.ResponseFriendRequest;

import java.nio.file.AccessDeniedException;
import java.util.List;

public interface FriendRequestService {
    void addFriendRequest(Long requesterId, Long addresseeId);

    void deleteRequestFriend(Long requesterId, Long addresseeId);

    List<FriendRequestDto> getOutgoingRequests(
            Long currentUserId,
            FriendRequestStatus status,
            int page,
            int size
    );

    List<FriendRequestDto> getIncomingRequests(Long currentUserId, int page, int size);

    void processFriendRequest(
            Long requestId,
            ResponseFriendRequest status,
            Long currentUserId
    ) throws AccessDeniedException;
}
