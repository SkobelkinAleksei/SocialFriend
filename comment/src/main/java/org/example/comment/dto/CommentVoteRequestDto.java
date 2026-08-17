package org.example.comment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentVoteRequestDto {
    @NotBlank
    private String vote;
}
