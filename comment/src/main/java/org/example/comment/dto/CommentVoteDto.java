package org.example.comment.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentVoteDto {
    private Long commentId;
    private long likesCount;
    private long dislikesCount;
    /** LIKE, DISLIKE или null */
    private String myVote;
}
