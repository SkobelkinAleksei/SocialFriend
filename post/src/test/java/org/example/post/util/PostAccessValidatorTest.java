package org.example.post.util;

import org.example.post.entity.PostEntity;
import org.example.post.exception.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("PostAccessValidator — только автор поста")
public class PostAccessValidatorTest {

    private final PostAccessValidator validator = new PostAccessValidator();

    @Test
    @DisplayName("Автор проходит проверку")
    void authorOk() {
        PostEntity post = PostEntity.builder().authorId(5L).build();
        assertDoesNotThrow(() -> validator.validateAuthor(post, 5L));
    }

    @Test
    @DisplayName("Чужой пользователь — ForbiddenException")
    void stranger() {
        PostEntity post = PostEntity.builder().authorId(5L).build();
        assertThrows(ForbiddenException.class, () -> validator.validateAuthor(post, 6L));
    }
}
