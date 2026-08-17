package org.example.user.validation;

public final class UserValidationRules {

    public static final String NAME =
            "^[A-Za-zА-Яа-яЁё]+(?:[ -][A-Za-zА-Яа-яЁё]+)*$";
    public static final String NAME_MESSAGE =
            "Только буквы, пробел и дефис (например Анна-Мария).";

    public static final String PHONE = "^\\+7[0-9]{10}$";
    public static final String PHONE_MESSAGE =
            "Телефон должен быть в формате +7 и 10 цифр.";

    public static final String PASSWORD = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,64}$";
    public static final String PASSWORD_MESSAGE =
            "Пароль: от 8 до 64 символов, заглавная, строчная буква и цифра.";

    private UserValidationRules() {
    }
}
