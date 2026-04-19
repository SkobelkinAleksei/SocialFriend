package org.example.friend.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.friend.dto.FriendRequestDto;
import org.example.friend.entity.FriendEntity;
import org.example.friend.entity.FriendRequestEntity;
import org.example.friend.entity.enums.FriendRequestStatus;
import org.example.friend.entity.enums.ResponseFriendRequest;
import org.example.friend.mapper.FriendRequestMapper;
import org.example.friend.repository.FriendRequestRepository;
import org.example.friend.repository.UserReferenceRepository;
import org.example.friend.service.FriendRequestService;
import org.example.friend.service.PrivateFriendService;
import org.example.friend.util.FriendRequestSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendRequestServiceImpl implements FriendRequestService {
    private final FriendRequestRepository friendRequestRepository;
    private final PrivateFriendService privateFriendService;
    private final FriendRequestMapper friendRequestMapper;
    private final UserReferenceRepository userReferenceRepository;

    @Transactional
    public void addFriendRequest(Long requesterId, Long addresseeId) {

        if (requesterId.equals(addresseeId)) {
            throw new IllegalArgumentException("Нельзя добавить себя в друзья");
        }

        if (!userReferenceRepository.existsById(addresseeId)) {
            throw new EntityNotFoundException("Такой пользователь не существует");
        }

        friendRequestRepository.findBetweenUsers(
                requesterId,
                addresseeId
        ).ifPresent(ex -> {
            throw new IllegalStateException(
                    "Связь между пользователями уже существует"
            );
        });

        FriendRequestEntity friendRequestEntity = new FriendRequestEntity();
        friendRequestEntity.setRequesterId(requesterId);
        friendRequestEntity.setAddresseeId(addresseeId);
        friendRequestEntity.setStatus(FriendRequestStatus.PENDING);

        friendRequestRepository.save(friendRequestEntity);
        log.info("[FriendRequestService - INFO] Заявка в друзья успешно создана");
    }

    @Transactional
    public void deleteRequestFriend(Long requesterId, Long addresseeId) {

        FriendRequestEntity requestEntity = friendRequestRepository
                .findByRequesterIdAndAddresseeId(requesterId, addresseeId)
                .orElseThrow(() -> new EntityNotFoundException("Заявка не была найдена"));

        if (requestEntity.getStatus().equals(FriendRequestStatus.ACCEPTED)) {
            throw new IllegalStateException("Заявка уже была принята.");
        }

        friendRequestRepository.deleteById(requestEntity.getId());
        log.info("[FriendRequestService - INFO] Заявка в друзья успешно удалена");
    }

    @Transactional(readOnly = true)
    public List<FriendRequestDto> getOutgoingRequests(
            Long currentUserId,
            FriendRequestStatus status,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Specification<FriendRequestEntity> spec = FriendRequestSpecification.requestsByStatus(currentUserId, status);
        Page<FriendRequestEntity> requestsPage = friendRequestRepository.findAll(spec, pageable);

        return requestsPage.getContent()
                .stream()
                .map(friendRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendRequestDto> getIncomingRequests(Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<FriendRequestEntity> spec = FriendRequestSpecification
                .incomingPendingRequests(currentUserId);

        Page<FriendRequestEntity> requestsPage = friendRequestRepository
                .findAll(spec, pageable);

        return requestsPage.getContent()
                .stream()
                .map(friendRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void processFriendRequest(
                    Long requestId,
                    ResponseFriendRequest status,
                    Long currentUserId
    ) throws AccessDeniedException {
        if (isNull(status)) {
            throw new IllegalArgumentException("Новый статус не может быть Null!");
        }

        FriendRequestEntity requestEntity = friendRequestRepository.findById(requestId).orElseThrow(
                () -> new EntityNotFoundException("Запрос не был найден!"));

        if (!requestEntity.getAddresseeId().equals(currentUserId)) {
            throw new AccessDeniedException("Только владелец может изменять свои входящие заявки");
        }

        if (status.equals(ResponseFriendRequest.ACCEPTED)) {

            long user1 = Math.min(currentUserId, requestEntity.getRequesterId());
            long user2 = Math.max(currentUserId, requestEntity.getRequesterId());

            requestEntity.setStatus(FriendRequestStatus.ACCEPTED);

            FriendEntity friendEntity = new FriendEntity();
            friendEntity.setUserId1(user1);
            friendEntity.setUserId2(user2);
            privateFriendService.createFriendship(friendEntity);

            log.info("[FriendRequestService - INFO] Заявка в друзья успешно принята");

        } else if (status.equals(ResponseFriendRequest.REJECTED)) {
            requestEntity.setStatus(FriendRequestStatus.REJECTED);

            log.info("[FriendRequestService - INFO] Заявка в друзья успешно отклонена");
        }
    }
}
