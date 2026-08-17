package org.example.friend.service.impl;

import org.example.friend.entity.FriendEntity;
import org.example.friend.mapper.FriendMapper;
import org.example.friend.repository.FriendRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FriendServiceImpl — проверка дружбы")
public class FriendServiceImplTest {

    @Mock
    private FriendRepository friendRepository;
    @Mock
    private FriendMapper friendMapper;

    @InjectMocks
    private FriendServiceImpl service;

    @Test
    @DisplayName("areFriends не зависит от порядка id")
    void orderIndependent() {
        when(friendRepository.findEntityByUserId1AndUserId2(3L, 9L))
                .thenReturn(Optional.of(new FriendEntity()));

        assertTrue(service.areFriends(9L, 3L));
        verify(friendRepository).findEntityByUserId1AndUserId2(3L, 9L);
    }

    @Test
    @DisplayName("Нет записи — не друзья")
    void notFriends() {
        when(friendRepository.findEntityByUserId1AndUserId2(1L, 2L)).thenReturn(Optional.empty());
        assertFalse(service.areFriends(1L, 2L));
    }
}
