package org.example.chat.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CreatePollRequest {
    @NotBlank(message = "Введите вопрос голосования")
    @Size(max = 255, message = "Вопрос не длиннее 255 символов")
    private String question;

    @Size(min = 2, max = 10, message = "Нужно от 2 до 10 вариантов ответа")
    private List<@NotBlank(message = "Вариант не может быть пустым") @Size(max = 100, message = "Вариант не длиннее 100 символов") String> options = new ArrayList<>();

    private boolean anonymous = true;
    private boolean multiple = false;
}
