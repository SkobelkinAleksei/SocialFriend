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
    private Long id;
    private Long requesterId;
    private Long addresseeId;
    private FriendRequestStatus status;
}