package org.example.friend.service.impl;

import org.example.friend.entity.FriendBlockListEntity;
import org.example.friend.entity.FriendEntity;
import org.example.friend.entity.FriendRequestEntity;
import org.example.friend.entity.enums.FriendRequestStatus;
import org.example.friend.repository.FriendBlockListRepository;
import org.example.friend.repository.FriendRepository;
import org.example.friend.repository.FriendRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlockServiceImpl — личная блокировка соседа")
class BlockServiceImplTest {

    @Mock
    private FriendBlockListRepository blockListRepository;
    @Mock
    private FriendRepository friendRepository;
    @Mock
    private FriendRequestRepository friendRequestRepository;

    @InjectMocks
    private BlockServiceImpl service;

    @Test
    @DisplayName("Нельзя заблокировать себя")
    void cannotBlockSelf() {
        assertThrows(IllegalArgumentException.class, () -> service.block(1L, 1L));
        verify(blockListRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Блок рвёт дружбу и удаляет заявку")
    void blockBreaksTies() {
        when(blockListRepository.existsByAuthorIdAndBlockedUserId(1L, 2L)).thenReturn(false);
        FriendEntity friend = new FriendEntity();
        friend.setId(9L);
        when(friendRepository.findEntityByUserId1AndUserId2(1L, 2L)).thenReturn(Optional.of(friend));
        FriendRequestEntity request = FriendRequestEntity.builder()
                .id(4L)
                .requesterId(1L)
                .addresseeId(2L)
                .status(FriendRequestStatus.PENDING)
                .build();
        when(friendRequestRepository.findRequestBetweenUsers(1L, 2L)).thenReturn(Optional.of(request));

        service.block(1L, 2L);

        verify(blockListRepository).saveAndFlush(any(FriendBlockListEntity.class));
        verify(friendRepository).deleteById(9L);
        verify(friendRequestRepository).delete(request);
    }

    @Test
    @DisplayName("Повторный блок — без второй записи")
    void idempotentBlock() {
        when(blockListRepository.existsByAuthorIdAndBlockedUserId(1L, 2L)).thenReturn(true);
        service.block(1L, 2L);
        verify(blockListRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("hiddenUserIds собирает обе стороны")
    void hiddenIds() {
        when(blockListRepository.findByAuthorId(1L)).thenReturn(List.of(
                FriendBlockListEntity.builder().authorId(1L).blockedUserId(2L).build()
        ));
        when(blockListRepository.findByBlockedUserId(1L)).thenReturn(List.of(
                FriendBlockListEntity.builder().authorId(8L).blockedUserId(1L).build()
        ));

        Set<Long> ids = service.hiddenUserIds(1L);

        assertEquals(Set.of(2L, 8L), ids);
        assertTrue(service.status(1L, 1L).isEitherWay() == false);
    }

    @Test
    @DisplayName("status: я заблокировал / меня заблокировали")
    void statusDirections() {
        when(blockListRepository.existsByAuthorIdAndBlockedUserId(1L, 2L)).thenReturn(true);
        when(blockListRepository.existsByAuthorIdAndBlockedUserId(2L, 1L)).thenReturn(false);

        var status = service.status(1L, 2L);

        assertTrue(status.isBlockedByMe());
        assertFalse(status.isBlockedMe());
        assertTrue(status.isEitherWay());
    }
}
