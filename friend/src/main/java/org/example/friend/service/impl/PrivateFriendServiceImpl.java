package org.example.friend.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.friend.entity.FriendEntity;
import org.example.friend.entity.enums.FriendRequestStatus;
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
        friendRepository.deleteById(friendEntity.getId());

        friendRequestRepository.findRequestBetweenUsers(currentUserId, userId2).ifPresent(requestEntity -> {
            requestEntity.setStatus(FriendRequestStatus.REJECTED);

            requestEntity.setRequesterId(userId2);
            requestEntity.setAddresseeId(currentUserId);

            friendRequestRepository.save(requestEntity);
            log.info("[PrivateFriendServiceImpl - INFO] Связь успешно переведена в статус REJECTED (Подписка)");
        });

        log.info("[PrivateFriendServiceImpl - INFO] Дружба была успешно разорвана.");
    }

    @Override
    @Transactional
    public void createFriendship(FriendEntity friendEntity) {
        long u1 = Math.min(friendEntity.getUserId1(), friendEntity.getUserId2());
        long u2 = Math.max(friendEntity.getUserId1(), friendEntity.getUserId2());
        friendEntity.setUserId1(u1);
        friendEntity.setUserId2(u2);

        if (friendRepository.findEntityByUserId1AndUserId2(u1, u2).isPresent()) {
            log.info("[PrivateFriendServiceImpl] Дружба уже существует: {}-{}", u1, u2);
            return;
        }

        friendRepository.save(friendEntity);
        log.info("[PrivateFriendServiceImpl - INFO] Дружба успешно создана");
    }
}

