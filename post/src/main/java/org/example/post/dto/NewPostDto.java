package org.example.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class NewPostDto implements Serializable {

    @Size(min = 5, max = 100, message = "Длина [CONTENT] должна быть от 5 до 100 символов")
    @NotBlank(message = "[CONTENT] не может быть пустым")
    private String content;

    @NotNull(message = "Нужно указать, разрешены ли комментарии.")
    private boolean commentsAllowed;
}
