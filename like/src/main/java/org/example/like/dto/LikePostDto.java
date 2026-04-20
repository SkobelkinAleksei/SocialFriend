package org.example.like.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class LikePostDto implements Serializable {
    Long userId;
    LocalDateTime createdAt;
}