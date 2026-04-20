package org.example.comment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class NewCommentDto implements Serializable {
    @NotNull(message = "Комментарий не должен быть NULL!")
    @Size(message = "Минимальный размер комментария от 2 до 1000", min = 2, max = 100)
    String content;
}