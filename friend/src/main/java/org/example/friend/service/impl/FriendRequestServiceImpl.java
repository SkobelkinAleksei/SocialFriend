package org.example.friend.service.impl;

import com.example.common.RequestData;
import com.example.common.dto.event.UserDto;
import com.example.common.kafka.NotificationKafkaProducer;
import com.example.common.kafka.NotificationType;
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
import org.example.friend.service.BlockService;
import org.example.friend.service.FriendRequestService;
import org.example.friend.service.FriendService;
import org.example.friend.service.PrivateFriendService;
import org.example.friend.util.FriendRequestSpecification;
import org.example.restclient.config.IHttpCore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
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
    private final FriendService friendService;
    private final FriendRequestMapper friendRequestMapper;
    private final UserReferenceRepository userReferenceRepository;
    private final NotificationKafkaProducer notificationProducer;
    private final IHttpCore httpCore;
    private final BlockService blockService;

    @Value("${app.services.user-base-url:http://localhost:8081}")
    private String userBaseUrl;

    @Override
    @Transactional
    public void addFriendRequest(Long requesterId, Long addresseeId) {

        if (requesterId.equals(addresseeId)) {
            throw new IllegalArgumentException("Нельзя добавить себя в друзья");
        }

        if (!userReferenceRepository.existsById(addresseeId)) {
            throw new EntityNotFoundException("Такой пользователь не существует");
        }

        if (blockService.isBlockedEitherWay(requesterId, addresseeId)) {
            throw new IllegalStateException("Нельзя отправить заявку: пользователь в чёрном списке");
        }

        if (Boolean.TRUE.equals(friendService.areFriends(requesterId, addresseeId))) {
            throw new IllegalStateException("Вы уже друзья");
        }

        friendRequestRepository.findRequestBetweenUsers(
                requesterId,
                addresseeId
        ).ifPresent(ex -> {
            if (ex.getStatus() == FriendRequestStatus.PENDING) {
                throw new IllegalStateException("Заявка в друзья уже отправлена");
            }
            if (ex.getStatus() == FriendRequestStatus.REJECTED) {
                throw new IllegalStateException("Вы уже подписаны на этого пользователя");
            }
            throw new IllegalStateException("Связь между пользователями уже существует");
        });

        FriendRequestEntity friendRequestEntity = new FriendRequestEntity();
        friendRequestEntity.setRequesterId(requesterId);
        friendRequestEntity.setAddresseeId(addresseeId);
        friendRequestEntity.setStatus(FriendRequestStatus.PENDING);

        friendRequestRepository.save(friendRequestEntity);
        log.info("[FriendRequestService - INFO] Заявка в друзья успешно создана");

        String requesterFirstName = "Житель";
        String requesterLastName = "Района";

        try {
            // Прямой вызов user-сервиса (без gateway JWT)
            String userUrl = userBaseUrl + "/api/v1/social/users/" + requesterId;
            RequestData requestData = new RequestData(userUrl);
            ResponseEntity<UserDto> response = httpCore.get(requestData, UserDto.class);

            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                UserDto userDto = response.getBody();
                requesterFirstName = userDto.getFirstName();
                requesterLastName = userDto.getLastName();
            }
        } catch (Exception e) {
            log.error("[FriendRequestService] Ошибка вычитки ФИО инициатора заявки {}, используем фолбэк", requesterId, e);
        }

        // Отправляем событие с новой сигнатурой (7 параметров)
        notificationProducer.sendEvent(
                addresseeId,                                             // receiverId (кому пуш)
                requesterId,                                             // senderId (от кого)
                requesterFirstName,                                      // Имя для ссылки на фронте
                requesterLastName,                                       // Фамилия для ссылки
                NotificationType.FRIEND_REQUEST_SENT,                    // Тип события
                null,                                                    // targetId = null, так как переходим в профиль автора
                null,
                "хочет добавиться к вам в друзья"                        // Текст уведомления
        );
    }

    @Override
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
    public List<FriendRequestDto> getOutgoingRequests(
            Long currentUserId,
            FriendRequestStatus status,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(boundedPage(page), boundedSize(size), Sort.by("createdAt").descending());
        Specification<FriendRequestEntity> spec = FriendRequestSpecification.requestsByStatus(currentUserId, status);
        Page<FriendRequestEntity> requestsPage = friendRequestRepository.findAll(spec, pageable);

        return requestsPage.getContent()
                .stream()
                .map(friendRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendRequestDto> getIncomingRequests(Long currentUserId, FriendRequestStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(boundedPage(page), boundedSize(size), Sort.by("createdAt").descending());

        // Если фронтенд забыл прислать статус, по умолчанию считаем PENDING (новые входящие заявки)
        FriendRequestStatus targetStatus = (status == null) ? FriendRequestStatus.PENDING : status;

        // Вызываем нашу новую правильную входящую спецификацию
        Specification<FriendRequestEntity> spec = FriendRequestSpecification.incomingRequestsByStatus(currentUserId, targetStatus);

        Page<FriendRequestEntity> requestsPage = friendRequestRepository.findAll(spec, pageable);
        return requestsPage.getContent()
                .stream()
                .map(friendRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
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
            // Принять можно из PENDING (заявка) или REJECTED (подписчик)
            if (requestEntity.getStatus() != FriendRequestStatus.PENDING
                    && requestEntity.getStatus() != FriendRequestStatus.REJECTED) {
                throw new IllegalStateException("Заявку в этом статусе нельзя принять");
            }

            if (Boolean.TRUE.equals(friendService.areFriends(currentUserId, requestEntity.getRequesterId()))) {
                throw new IllegalStateException("Вы уже друзья");
            }
            if (blockService.isBlockedEitherWay(currentUserId, requestEntity.getRequesterId())) {
                throw new IllegalStateException("Нельзя принять заявку: пользователь в чёрном списке");
            }

            long user1 = Math.min(currentUserId, requestEntity.getRequesterId());
            long user2 = Math.max(currentUserId, requestEntity.getRequesterId());

            requestEntity.setStatus(FriendRequestStatus.ACCEPTED);

            FriendEntity friendEntity = new FriendEntity();
            friendEntity.setUserId1(user1);
            friendEntity.setUserId2(user2);
            privateFriendService.createFriendship(friendEntity);

            log.info("[FriendRequestService - INFO] Заявка в друзья успешно принята");

            String accepterFirstName = "Житель";
            String accepterLastName = "Района";

            try {
                String userUrl = userBaseUrl + "/api/v1/social/users/" + currentUserId;
                RequestData requestData = new RequestData(userUrl);
                ResponseEntity<UserDto> response = httpCore.get(requestData, UserDto.class);

                if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    UserDto userDto = response.getBody();
                    accepterFirstName = userDto.getFirstName();
                    accepterLastName = userDto.getLastName();
                }
            } catch (Exception e) {
                log.error("[FriendRequestService] Ошибка вычитки ФИО одобряющего пользователя {}, используем фолбэк", currentUserId, e);
            }

            notificationProducer.sendEvent(
                    requestEntity.getRequesterId(),
                    currentUserId,
                    accepterFirstName,
                    accepterLastName,
                    NotificationType.FRIEND_REQUEST_ACCEPTED,
                    null,
                    null,
                    "принял(а) ваш запрос в друзья"
            );
        } else if (status.equals(ResponseFriendRequest.REJECTED)) {
            // Отклонение заявки → подписка; только из PENDING
            if (requestEntity.getStatus() != FriendRequestStatus.PENDING) {
                throw new IllegalStateException("Отклонить можно только ожидающую заявку");
            }
            requestEntity.setStatus(FriendRequestStatus.REJECTED);
            log.info("[FriendRequestService - INFO] Заявка в друзья успешно отклонена (подписка)");
        }
    }

    @Override
    public long countSubscribers(Long userId, FriendRequestStatus friendRequestStatus) {
        return friendRequestRepository.countByAddresseeIdAndStatus(userId, friendRequestStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public long countIncomingRequests(Long currentUserId, FriendRequestStatus status) {
        FriendRequestStatus targetStatus = (status == null) ? FriendRequestStatus.PENDING : status;
        return friendRequestRepository.count(
                FriendRequestSpecification.incomingRequestsByStatus(currentUserId, targetStatus)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long countOutgoingRequests(Long currentUserId, FriendRequestStatus status) {
        return friendRequestRepository.count(
                FriendRequestSpecification.requestsByStatus(currentUserId, status)
        );
    }
}
