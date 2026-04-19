package org.example.friend.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.friend.entity.FriendEntity;
import org.example.friend.repository.FriendRepository;
import org.example.friend.repository.FriendRequestRepository;
import org.example.friend.service.PrivateFriendService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class PrivateFriendServiceImpl implements PrivateFriendService {
    private final FriendRepository friendRepository;
    private final FriendRequestRepository friendRequestRepository;

    @Override
    @Transactional
    public void deleteFriendById(Long currentUserId, Long userId2) {

        long user1 = Math.min(currentUserId, userId2);
        long user2 = Math.max(currentUserId, userId2);

        Optional<FriendEntity> friendOpt = friendRepository.findEntityByUserId1AndUserId2(user1, user2);
        if (friendOpt.isEmpty()) {
            throw new EntityNotFoundException("Дружба между пользователями не найдена");
        }

        FriendEntity friendEntity = friendOpt.get();

        Optional<Long> optionalLong = friendRequestRepository
                .findRequestIdByAddresseeIdAndRequesterId(currentUserId, userId2);

        optionalLong.ifPresent(requestId -> {
            friendRequestRepository.deleteById(requestId);
            log.info("[PrivateFriendServiceImpl - INFO] Request {} удалён", requestId);
        });

        friendRepository.deleteById(friendEntity.getId());
        log.info("[PrivateFriendServiceImpl - INFO] Дружба была разорвана.");
    }

    @Override
    @Transactional
    public void createFriendship(FriendEntity friendEntity) {
        friendRepository.save(friendEntity);
        log.info("[PrivateFriendServiceImpl - INFO] Дружба успешно создана");
    }
}
