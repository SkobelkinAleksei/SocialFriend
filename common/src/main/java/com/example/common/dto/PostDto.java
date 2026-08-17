package com.example.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class PostDto implements Serializable {

    Long id;

    @NotNull(message = "Пользователь должен быть указан.")
    Long authorId;

    @NotBlank(message = "Содержание контента не может быть пустым или состоять только из пробелов.")
    @Size(min = 5, max = 3000, message = "Содержание контента не может быть менее 5 и более 3000 символов.")
    String content;

    @Size(max = 10, message = "Можно прикрепить не больше 10 фотографий.")
    List<String> photos;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt;

    boolean commentsAllowed;
    boolean canComment;
    long viewsCount;
    long likesCount;
    @JsonProperty("isLiked")
    boolean isLiked;
    long commentsCount;

    /** PUBLISHED / REMOVED — для внутренних проверок (лайк, комментарии). */
    String statusPost;
}