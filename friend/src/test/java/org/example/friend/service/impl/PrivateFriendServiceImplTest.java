package org.example.friend.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.example.friend.entity.FriendEntity;
import org.example.friend.entity.FriendRequestEntity;
import org.example.friend.entity.enums.FriendRequestStatus;
import org.example.friend.repository.FriendRepository;
import org.example.friend.repository.FriendRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrivateFriendServiceImpl — создание и разрыв дружбы")
public class PrivateFriendServiceImplTest {

    @Mock
    private FriendRepository friendRepository;
    @Mock
    private FriendRequestRepository friendRequestRepository;

    @InjectMocks
    private PrivateFriendServiceImpl service;

    @Test
    @DisplayName("createFriendship нормализует порядок id (меньший всегда userId1)")
    void canonicalOrder() {
        when(friendRepository.findEntityByUserId1AndUserId2(3L, 10L)).thenReturn(Optional.empty());
        FriendEntity incoming = FriendEntity.builder().userId1(10L).userId2(3L).build();

        service.createFriendship(incoming);

        assertEquals(3L, incoming.getUserId1());
        assertEquals(10L, incoming.getUserId2());
        verify(friendRepository).save(incoming);
    }

    @Test
    @DisplayName("Повторное создание той же пары — идемпотентный skip")
    void alreadyExists() {
        when(friendRepository.findEntityByUserId1AndUserId2(3L, 10L))
                .thenReturn(Optional.of(FriendEntity.builder().id(1L).userId1(3L).userId2(10L).build()));

        service.createFriendship(FriendEntity.builder().userId1(10L).userId2(3L).build());

        verify(friendRepository, never()).save(any());
    }

    @Test
    @DisplayName("Удаление несуществующей дружбы — EntityNotFoundException")
    void deleteMissing() {
        when(friendRepository.findEntityByUserId1AndUserId2(1L, 2L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.deleteFriendById(2L, 1L));
    }

    @Test
    @DisplayName("Разрыв дружбы: запись удаляется, заявка становится REJECTED в пользу бывшего друга")
    void deleteTurnsIntoSubscription() {
        FriendEntity friendship = FriendEntity.builder().id(7L).userId1(1L).userId2(4L).build();
        when(friendRepository.findEntityByUserId1AndUserId2(1L, 4L)).thenReturn(Optional.of(friendship));
        FriendRequestEntity request = FriendRequestEntity.builder()
                .id(9L)
                .requesterId(1L)
                .addresseeId(4L)
                .status(FriendRequestStatus.ACCEPTED)
                .build();
        when(friendRequestRepository.findRequestBetweenUsers(1L, 4L)).thenReturn(Optional.of(request));

        service.deleteFriendById(1L, 4L);

        verify(friendRepository).deleteById(7L);
        assertEquals(FriendRequestStatus.REJECTED, request.getStatus());
        assertEquals(4L, request.getRequesterId());
        assertEquals(1L, request.getAddresseeId());
        verify(friendRequestRepository).save(request);
    }
}
