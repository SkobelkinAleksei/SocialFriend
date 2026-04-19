package org.example.friend.dto;

import lombok.*;
import org.example.friend.entity.enums.FriendRequestStatus;

import java.io.Serializable;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestDto implements Serializable {
    Long id;
    Long requesterId;
    Long addresseeId;
    FriendRequestStatus status;
}