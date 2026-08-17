package org.example.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;
import java.util.List;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostDto implements Serializable {

    @Size(min = 5, max = 3000, message = "Длина [CONTENT] должна быть от 5 до 3000 символов")
    @NotBlank(message = "[CONTENT] не может быть пустым")
    String content;

    @Size(max = 10, message = "Можно прикрепить не больше 10 фотографий.")
    private List<String> photos;
}
