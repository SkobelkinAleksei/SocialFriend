package org.example.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class NewCommentDto implements Serializable {

    @NotBlank(message = "Комментарий не может быть пустым")
    @Size(min = 2, max = 1000, message = "Длина комментария должна быть от 2 до 1000 символов")
    String content;

    Long replyToUserId;
    Long replyToCommentId;
}
