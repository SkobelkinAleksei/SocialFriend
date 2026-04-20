package org.example.friend.dto;

import lombok.*;

import java.io.Serializable;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class FriendDto implements Serializable {
    Long userId1;
    Long userId2;
}