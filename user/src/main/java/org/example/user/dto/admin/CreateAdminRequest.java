package org.example.user.dto.admin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAdminRequest {
    /** Если задан — существующего соседа делают админом. Иначе создаётся новый служебный аккаунт. */
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String numberPhone;
    private String password;
}
