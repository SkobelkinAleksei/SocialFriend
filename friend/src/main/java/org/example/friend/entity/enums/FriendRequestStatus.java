package org.example.friend.entity.enums;

import lombok.Getter;

@Getter
public enum FriendRequestStatus {
    PENDING,   // запрос отправлен
    ACCEPTED,  // друзья
    REJECTED  // отказ
}
