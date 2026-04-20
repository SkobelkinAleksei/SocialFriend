package com.example.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class PostDto implements Serializable {

    @NotNull(message = "Пользователь должен быть указан.")
    Long authorId;

    @NotBlank(message = "Содержание контента не может быть пустым или состоять только из пробелов.")
    @Size(min = 5, max = 3000, message = "Содержание контента не может быть менее 5 и более 3000 символов.")
    String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt;

    boolean commentsAllowed;
}