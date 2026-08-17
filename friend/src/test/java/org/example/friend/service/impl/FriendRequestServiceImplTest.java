package org.example.friend.service.impl;

import com.example.common.kafka.NotificationKafkaProducer;
import com.example.common.kafka.NotificationType;
import jakarta.persistence.EntityNotFoundException;
import org.example.friend.entity.FriendRequestEntity;
import org.example.friend.entity.enums.FriendRequestStatus;
import org.example.friend.entity.enums.ResponseFriendRequest;
import org.example.friend.mapper.FriendRequestMapper;
import org.example.friend.repository.FriendRequestRepository;
import org.example.friend.repository.UserReferenceRepository;
import org.example.friend.service.BlockService;
import org.example.friend.service.FriendService;
import org.example.friend.service.PrivateFriendService;
import org.example.restclient.config.IHttpCore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.AccessDeniedException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FriendRequestServiceImpl — заявки в друзья")
public class FriendRequestServiceImplTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;
    @Mock
    private PrivateFriendService privateFriendService;
    @Mock
    private FriendService friendService;
    @Mock
    private FriendRequestMapper friendRequestMapper;
    @Mock
    private UserReferenceRepository userReferenceRepository;
    @Mock
    private NotificationKafkaProducer notificationProducer;
    @Mock
    private IHttpCore httpCore;

    @Mock
    private BlockService blockService;

    @InjectMocks
    private FriendRequestServiceImpl service;

    @BeforeEach
    void urls() {
        ReflectionTestUtils.setField(service, "userBaseUrl", "http://user");
        lenient().when(blockService.isBlockedEitherWay(anyLong(), anyLong())).thenReturn(false);
    }

    @Nested
    @DisplayName("Отправка заявки")
    class Add {

        @Test
        @DisplayName("Нельзя отправить заявку самому себе")
        void self() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.addFriendRequest(1L, 1L)
            );
            assertEquals("Нельзя добавить себя в друзья", ex.getMessage());
            verify(friendRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Адресата нет — EntityNotFoundException")
        void missingUser() {
            when(userReferenceRepository.existsById(9L)).thenReturn(false);
            assertThrows(EntityNotFoundException.class, () -> service.addFriendRequest(1L, 9L));
        }

        @Test
        @DisplayName("Уже друзья — конфликт")
        void alreadyFriends() {
            when(userReferenceRepository.existsById(2L)).thenReturn(true);
            when(friendService.areFriends(1L, 2L)).thenReturn(true);
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.addFriendRequest(1L, 2L));
            assertEquals("Вы уже друзья", ex.getMessage());
        }

        @Test
        @DisplayName("Блок в любую сторону — заявку не создаём")
        void blocked() {
            when(userReferenceRepository.existsById(2L)).thenReturn(true);
            when(blockService.isBlockedEitherWay(1L, 2L)).thenReturn(true);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.addFriendRequest(1L, 2L));
            assertEquals("Нельзя отправить заявку: пользователь в чёрном списке", ex.getMessage());
            verify(friendRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Повтор PENDING — «заявка уже отправлена»")
        void duplicatePending() {
            when(userReferenceRepository.existsById(2L)).thenReturn(true);
            when(friendService.areFriends(1L, 2L)).thenReturn(false);
            FriendRequestEntity existing = request(1L, 2L, FriendRequestStatus.PENDING);
            when(friendRequestRepository.findRequestBetweenUsers(1L, 2L)).thenReturn(Optional.of(existing));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.addFriendRequest(1L, 2L));
            assertEquals("Заявка в друзья уже отправлена", ex.getMessage());
        }

        @Test
        @DisplayName("Успех: PENDING сохраняется, уведомление уходит адресату даже если user-сервис упал")
        void successWithFallbackName() {
            when(userReferenceRepository.existsById(2L)).thenReturn(true);
            when(friendService.areFriends(1L, 2L)).thenReturn(false);
            when(friendRequestRepository.findRequestBetweenUsers(1L, 2L)).thenReturn(Optional.empty());
            when(httpCore.get(any(), eq(com.example.common.dto.event.UserDto.class)))
                    .thenThrow(new RuntimeException("user down"));

            service.addFriendRequest(1L, 2L);

            verify(friendRequestRepository).save(any(FriendRequestEntity.class));
            verify(notificationProducer).sendEvent(
                    eq(2L), eq(1L), eq("Житель"), eq("Района"),
                    eq(NotificationType.FRIEND_REQUEST_SENT),
                    eq(null), eq(null), eq("хочет добавиться к вам в друзья")
            );
        }
    }

    @Nested
    @DisplayName("Принятие / отклонение")
    class Process {

        @Test
        @DisplayName("Чужая входящая заявка — AccessDeniedException")
        void notOwner() {
            FriendRequestEntity entity = request(1L, 2L, FriendRequestStatus.PENDING);
            entity.setId(10L);
            when(friendRequestRepository.findById(10L)).thenReturn(Optional.of(entity));

            assertThrows(AccessDeniedException.class,
                    () -> service.processFriendRequest(10L, ResponseFriendRequest.ACCEPTED, 99L));
            verify(privateFriendService, never()).createFriendship(any());
        }

        @Test
        @DisplayName("Принятие PENDING создаёт дружбу с каноническим порядком id")
        void acceptCreatesFriendship() throws Exception {
            FriendRequestEntity entity = request(5L, 2L, FriendRequestStatus.PENDING);
            entity.setId(10L);
            when(friendRequestRepository.findById(10L)).thenReturn(Optional.of(entity));
            when(friendService.areFriends(2L, 5L)).thenReturn(false);

            service.processFriendRequest(10L, ResponseFriendRequest.ACCEPTED, 2L);

            assertEquals(FriendRequestStatus.ACCEPTED, entity.getStatus());
            verify(privateFriendService).createFriendship(any());
            verify(notificationProducer).sendEvent(
                    eq(5L), eq(2L), any(), any(),
                    eq(NotificationType.FRIEND_REQUEST_ACCEPTED),
                    eq(null), eq(null), eq("принял(а) ваш запрос в друзья")
            );
        }

        @Test
        @DisplayName("Отклонение PENDING переводит в REJECTED (подписка), дружбу не создаём")
        void rejectBecomesSubscriber() throws Exception {
            FriendRequestEntity entity = request(5L, 2L, FriendRequestStatus.PENDING);
            entity.setId(10L);
            when(friendRequestRepository.findById(10L)).thenReturn(Optional.of(entity));

            service.processFriendRequest(10L, ResponseFriendRequest.REJECTED, 2L);

            assertEquals(FriendRequestStatus.REJECTED, entity.getStatus());
            verify(privateFriendService, never()).createFriendship(any());
        }

        @Test
        @DisplayName("Отклонить уже принятую нельзя")
        void rejectAcceptedForbidden() {
            FriendRequestEntity entity = request(5L, 2L, FriendRequestStatus.ACCEPTED);
            entity.setId(10L);
            when(friendRequestRepository.findById(10L)).thenReturn(Optional.of(entity));

            assertThrows(IllegalStateException.class,
                    () -> service.processFriendRequest(10L, ResponseFriendRequest.REJECTED, 2L));
        }
    }

    @Test
    @DisplayName("Отмена заявки: ACCEPTED отменить нельзя")
    void cannotDeleteAccepted() {
        FriendRequestEntity entity = request(1L, 2L, FriendRequestStatus.ACCEPTED);
        entity.setId(3L);
        when(friendRequestRepository.findByRequesterIdAndAddresseeId(1L, 2L)).thenReturn(Optional.of(entity));

        assertThrows(IllegalStateException.class, () -> service.deleteRequestFriend(1L, 2L));
        verify(friendRequestRepository, never()).deleteById(anyLong());
    }

    private static FriendRequestEntity request(long from, long to, FriendRequestStatus status) {
        return FriendRequestEntity.builder()
                .requesterId(from)
                .addresseeId(to)
                .status(status)
                .build();
    }
}
